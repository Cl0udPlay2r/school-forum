package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.vo.response.WeatherVO;
import com.example.service.WeatherService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    RestTemplate rest;

    @Resource
    StringRedisTemplate template;
    // api key 27之后和风天气减少使用
//    @Value("${spring.weather.api-key}")
//    String key;
    //和风天气的api host
    @Value("${spring.weather.host}")
    String host;
    //对应凭证的项目id
    @Value("${spring.weather.project}")
    String projectId;
    //凭证id
    @Value("${spring.weather.jwt-key}")
    String kid;
    //私钥
    @Value("${spring.weather.privateKey}")
    String privateKeyString;

    /**
     * 依靠大概的经纬度获取天气信息
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return vo实体
     */
    @Override
    public WeatherVO fetchWeather(double longitude, double latitude) {
        return this.fetchFromCache(longitude,latitude);
    }

    /**
     * 为了节约资源 将获取某地理位置的天气信息放入缓存中
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 根据缓存查询到的vo实体
     */
    private WeatherVO fetchFromCache(double longitude, double latitude) {
        String url = "https://" + host + "/geo/v2/city/lookup?location=" +
                String.format("%.2f",longitude) + "," + String.format("%.2f",latitude);
        JSONObject geo = this.sendRequest(url);

        if (geo == null)return null;
        JSONObject location = geo.getJSONArray("location").getJSONObject(0);

        int id = location.getInteger("id");
        String key = Const.FORUM_WEATHER_CACHE + id;
        String cache = template.opsForValue().get(key);
        if (cache != null)
            return JSONObject.parseObject(cache).to(WeatherVO.class);
        WeatherVO vo = fetchFromAPI(id, location);
        if (vo == null)return null;
        template.opsForValue().set(key, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
        return vo;
    }
    /**
     * 当缓存中没有对应位置天气信息时 调用和风天气api获取实时和未来5小时天气信息
     * @param id 城市位置id
     * @param location 获取到的位置信息的JSON格式信息
     * @return 根据API查询到的vo实体
     */
    private WeatherVO fetchFromAPI(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);
        String urlNow = "https://" + host + "/v7/weather/now?location=" + id;

        JSONObject now = this.sendRequest(urlNow);
        if(now == null){
            System.out.println("now为空\n");
            return null;
        }
        vo.setNow(now.getJSONObject("now"));

        String urlFuture = "https://" + host + "/v7/weather/24h?location=" + id;
        JSONObject hourly = this.sendRequest(urlFuture);

        if(hourly == null){
            System.out.println("hourly为空\n");
            return null;
        }
        //我们只要5小时后的天气情况，多了不要顺便节约资源
        vo.setHourly(new JSONArray(hourly.getJSONArray("hourly").stream().limit(5).toList()));
        return vo;
    }

    /**
     * 由于每次请求和风天气的api都要携带token所以单独整个方法来请求，只需要给出请求的url即可
     * @param url 需要请求的api接口
     * @return 根据url查询道德json格式的数据
     */
    private JSONObject sendRequest(String url){
        HttpHeaders headers = new HttpHeaders();
        String token = "Bearer " + this.createJWT();
        headers.set("Authorization", token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Accept-Encoding", "gzip, deflate");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            byte[] responseBytes = rest.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();

            // 检查是否需要解压缩
            if (responseBytes != null) {
                return this.decompressStringToJson(responseBytes);
            }
            return null;
        } catch (HttpClientErrorException e) {
            System.out.println("Weather API request failed: " + e.getMessage());
            // 如果是错误响应，直接返回null而不是尝试解压缩
            return null;
        }
    }

    /**
     * 解压GZIP压缩的数据并转换为JSONObject
     *
     * @param compressedData 压缩的字节数组
     * @return 解压后的JSONObject
     */
    private JSONObject decompressStringToJson(byte[] compressedData) {
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedData));
            byte[] buffer = new byte[1024];
            int length;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((length = gzipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            gzipInputStream.close();
            outputStream.close();
            return JSONObject.parseObject(outputStream.toString());
        } catch (IOException e) {
            System.out.println("解压失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 创建符合和风天气的jwt以便发送请求
     * 方法官方有写
     * <a href="https://dev.qweather.com/docs/configuration/authentication/#json-web-token">jwt验证方法</a>
     * @return 返回创建的jwt
     */
    private String createJWT(){
        try {
            // Private key
//            privateKeyString = privateKeyString.replace("-----BEGIN PRIVATE KEY-----", "")
//                    .replace("-----END PRIVATE KEY-----", "").trim();
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Header
            String headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"" + kid +"\"}";

            // Payload
            long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30;
            long exp = iat + 900;
            String payloadJson = "{\"sub\": \"" + projectId + "\", \"iat\": " + iat + ", \"exp\": " + exp + "}";

            // Base64url header+payload
            String headerEncoded = Base64.getUrlEncoder().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String data = headerEncoded + "." + payloadEncoded;

            // Sign
            Signature signer = Signature.getInstance("EdDSA");
            signer.initSign(privateKey);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signature = signer.sign();

            String signatureEncoded = Base64.getUrlEncoder().encodeToString(signature);

            // Print Token
//            System.out.println("Signature:\n" + signatureEncoded);
//            System.out.println("JWT:\n" + jwt);

            return data + "." + signatureEncoded;
        }catch (Exception e){
            System.out.println("jwtCreate failed: "+e.getMessage());
            return null;
        }

    }

}
