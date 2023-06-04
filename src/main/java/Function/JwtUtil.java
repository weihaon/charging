package Function;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    public static String genJwt(String username, String password){
        Map<String,Object> claims = new HashMap<>();
        claims.put("username",username);
        claims.put("password",password);

        String jwt = Jwts.builder()
                .setClaims(claims) //自定义内容(载荷)
                .signWith(SignatureAlgorithm.HS256, "3fwNngLAN297XWDRv7QjqmD8FsF47krplskQHNofLa9SVaGbswookrUOIy3H") //签名算法
                .setExpiration(new Date(System.currentTimeMillis() + 24*3600*1000)) //有效期
                .compact();


        return jwt;
    }

    public static Claims getUserIdFromToken(String token){
        //去除前缀
        token = token.substring(6);

        Claims claims = Jwts.parser()
                .setSigningKey("3fwNngLAN297XWDRv7QjqmD8FsF47krplskQHNofLa9SVaGbswookrUOIy3H")//指定签名密钥（必须保证和生成令牌时使用相同的签名密钥）
                .parseClaimsJws(token)
                .getBody();


        return claims;
    }
}