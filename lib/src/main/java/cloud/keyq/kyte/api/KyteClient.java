package cloud.keyq.kyte.api;

import java.util.Base64;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class KyteClient {

    private String publicKey;
    private String privateKey;
    private String kyteAccount;
    private String kyteIdentifier;
    private String kyteEndpoint;
    private String kyteAppId;
    private String sessionToken = "0";
    private String transactionToken = "0";
    private String usernameField = "email";
    private String passwordField = "password";

    // Constructor
    public KyteClient(String publicKey, String privateKey, String kyteAccount, String kyteIdentifier, String kyteEndpoint, String kyteAppId) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.kyteAccount = kyteAccount;
        this.kyteIdentifier = kyteIdentifier;
        this.kyteEndpoint = kyteEndpoint;
        this.kyteAppId = kyteAppId;
    }

    private String encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmacSha256(String data, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
        sha256Hmac.init(secretKey);
        
        return sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getIdentity(String timestamp) throws Exception {
        String identityStr = publicKey + "%" + sessionToken + "%" + timestamp + "%" + kyteAccount;
        return URLEncoder.encode(encode(identityStr), "UTF-8");
    }

    public String getSignature(String epoch) throws NoSuchAlgorithmException, InvalidKeyException {
        String txToken = "0";
        byte[] key1 = hmacSha256(txToken, privateKey.getBytes(StandardCharsets.UTF_8));
        byte[] key2 = hmacSha256(kyteIdentifier, key1);
        return bytesToHex(hmacSha256(epoch, key2));
    }

    private JSONObject request(String method, String model, String field, String value, JSONObject data, Map<String, String> headers) throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        String epoch = String.valueOf(Instant.now().getEpochSecond());
        String timestamp = formatter.format(Instant.now());

        String signature = getSignature(epoch);
        String identity = getIdentity(timestamp);

        String endpoint = kyteEndpoint + "/" + model;
        if (field != null && value != null) {
            endpoint += "/" + field + "/" + value;
        }

        HttpClient client = HttpClients.createDefault();
        HttpUriRequest request;

        switch (method.toLowerCase()) {
            case "post":
                request = new HttpPost(endpoint);
                ((HttpPost) request).setEntity(new StringEntity(data.toString(), StandardCharsets.UTF_8));
                break;
            case "put":
                request = new HttpPut(endpoint);
                ((HttpPut) request).setEntity(new StringEntity(data.toString(), StandardCharsets.UTF_8));
                break;
            case "get":
                request = new HttpGet(endpoint);
                break;
            case "delete":
                request = new HttpDelete(endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        request.addHeader("x-kyte-signature", signature);
        request.addHeader("x-kyte-identity", identity);

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.addHeader(header.getKey(), header.getValue());
            }
        }

        if (kyteAppId != null) {
            request.addHeader("x-kyte-appid", kyteAppId);
        }

        HttpResponse response = client.execute(request);
        String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("HTTP Error: " + response.getStatusLine().getStatusCode() + " - " + jsonResponse);
        }

        return new JSONObject(jsonResponse);
    }

    public JSONObject post(String model, JSONObject data, Map<String, String> headers) throws Exception {
        return request("post", model, null, null, data, headers);
    }

    public JSONObject put(String model, String field, String value, JSONObject data, Map<String, String> headers) throws Exception {
        return request("put", model, field, value, data, headers);
    }

    public JSONObject get(String model, String field, String value, Map<String, String> headers) throws Exception {
        return request("get", model, field, value, null, headers);
    }

    public JSONObject delete(String model, String field, String value, Map<String, String> headers) throws Exception {
        return request("delete", model, field, value, null, headers);
    }

    public JSONObject createSession(String username, String password) throws Exception {
        JSONObject data = new JSONObject();
        data.put(usernameField, username);
        data.put(passwordField, password);
        JSONObject result = post("Session", data, null);
        sessionToken = result.getString("sessionToken");
        transactionToken = result.getString("transactionToken");
        return result;
    }
}
