import com.auth0.jwt.interfaces.DecodedJWT;
import com.yyh.commonlib.utils.JwtUtils;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilsTest {

    @Test
    public void testSignAndVerifyToken() {
        // 准备测试数据
        String userId = "3ef0e52f-c60f-42f6-b504-b3089bd1d834";
        String userName = "user01";
        String userRole = "ADMIN";

        // 调用 JwtUtils 签发 token
        String token = JwtUtils.SignToken(userId, userName, userRole);
        assertNotNull(token, "签发的 token 不应为空");
        System.out.println(token);
        // 调用 JwtUtils 验证 token
        DecodedJWT decodedJWT = JwtUtils.Verify(token);
        assertNotNull(decodedJWT, "验证后的 token 不应为空");

        // 验证 token 中的各个 Claim 是否与预期一致
        assertEquals(userId, decodedJWT.getClaim("x-user-id").asString(), "用户ID不匹配");
        assertEquals(userName, decodedJWT.getClaim("x-user-name").asString(), "用户名不匹配");
        assertEquals(userRole, decodedJWT.getClaim("x-user-role").asString(), "用户角色不匹配");
    }

    @Test
    public void testVerifyInvalidToken() {
        // 测试传入无效 token 时返回 null 的情况
        String invalidToken = "invalidToken";
        DecodedJWT decodedJWT = JwtUtils.Verify(invalidToken);
        assertNull(decodedJWT, "验证无效 token 时应返回 null");
    }
}
