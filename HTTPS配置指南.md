# HTTPS 配置指南

## 一、为什么 HTTPS 是更可靠的安全方案？

### 1. 安全机制对比

| 特性 | 前端 AES 加密 | HTTPS (TLS/SSL) |
|------|--------------|-----------------|
| **加密位置** | 应用层（JavaScript） | 传输层（TCP/IP） |
| **密钥管理** | 密钥暴露在前端代码中 ❌ | 密钥在握手时动态协商 ✅ |
| **防中间人攻击** | 无法防护 ❌ | 通过证书验证服务器身份 ✅ |
| **防篡改** | 无保护 ❌ | 有完整性校验 ✅ |
| **性能开销** | 前后端都需要加解密 | 浏览器/服务器底层优化 |
| **标准化** | 自定义实现 | 国际标准（RFC 8446） |

### 2. HTTPS 工作原理

```
客户端                   中间人                   服务器
  |                        |                        |
  |--- 1. 请求HTTPS连接 --->|                        |
  |                        |--- 2. 请求HTTPS连接 --->|
  |                        |<-- 3. 返回证书 ---------|
  |<-- 4. 返回证书 ---------|                        |
  |                        |                        |
  | 5. 验证证书（CA签名）   |                        |
  | 6. 生成随机密钥         |                        |
  | 7. 用服务器公钥加密密钥 |                        |
  |--- 8. 发送加密密钥 ---->|                        |
  |                        |--- 9. 发送加密密钥 ---->|
  |                        |                        |
  | 10. 使用对称密钥加密通信 |                        |
  |<=================== 加密数据传输 ===============>|
```

**关键点：**
- 密钥在握手时动态生成，不存储在代码中
- 证书由 CA（证书颁发机构）签发，验证服务器身份
- 即使中间人截获数据，也无法解密（没有私钥）

### 3. 前端 AES 加密的问题

```javascript
// 问题1：密钥硬编码在前端代码中
const SECRET_KEY = 'MyBlogSecretKey2025'  // ❌ 任何人都能看到

// 问题2：攻击者可以：
// 1. 查看浏览器源码 → 获取密钥
// 2. 使用密钥解密所有传输的数据
// 3. 甚至伪造加密数据发送给服务器
```

## 二、Spring Boot 配置 HTTPS

### 方案一：使用自签名证书（开发/测试环境）

#### 步骤 1：生成自签名证书

```bash
# 使用 keytool 生成证书（JDK 自带工具）
keytool -genkeypair \
  -alias blog-https \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore blog-keystore.p12 \
  -validity 365 \
  -dname "CN=localhost, OU=Blog, O=MyBlog, L=Beijing, ST=Beijing, C=CN" \
  -storepass blog123456 \
  -keypass blog123456
```

**参数说明：**
- `-alias`: 证书别名
- `-keyalg`: 密钥算法（RSA）
- `-keysize`: 密钥长度（2048位）
- `-keystore`: 密钥库文件名
- `-validity`: 有效期（365天）
- `-dname`: 证书信息（CN 必须与域名匹配）
- `-storepass`: 密钥库密码
- `-keypass`: 密钥密码

#### 步骤 2：将证书放到 resources 目录

```bash
# 将生成的证书文件复制到项目 resources 目录
cp blog-keystore.p12 blog-application/src/main/resources/
```

#### 步骤 3：配置 application.yml

```yaml
server:
  port: 8443  # HTTPS 默认端口 443，开发环境可用 8443
  address: 0.0.0.0
  ssl:
    enabled: true
    key-store: classpath:blog-keystore.p12
    key-store-password: blog123456
    key-store-type: PKCS12
    key-alias: blog-https
    # 可选：强制 HTTPS（禁用 HTTP）
    # http2:
    #   enabled: true
```

#### 步骤 4：同时支持 HTTP 和 HTTPS（可选）

如果需要同时支持 HTTP 和 HTTPS，需要配置两个端口：

**application.yml:**
```yaml
server:
  port: 8088  # HTTP 端口
  address: 0.0.0.0

# HTTPS 配置（通过代码配置）
```

**创建 HTTPS 配置类：**
```java
package cn.lzx.blog.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpsConfig {

    @Value("${server.ssl.key-store}")
    private String keyStore;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(org.apache.catalina.Context context) {
                org.apache.catalina.deploy.SecurityConstraint securityConstraint = 
                    new org.apache.catalina.deploy.SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                org.apache.catalina.deploy.SecurityCollection collection = 
                    new org.apache.catalina.deploy.SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(createSslConnector());
        return tomcat;
    }

    private Connector createSslConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setPort(8443);
        connector.setProperty("SSLEnabled", "true");
        connector.setProperty("keystoreFile", 
            getClass().getClassLoader().getResource(keyStore).getFile());
        connector.setProperty("keystorePass", keyStorePassword);
        connector.setProperty("keystoreType", keyStoreType);
        connector.setProperty("keyAlias", keyAlias);
        return connector;
    }
}
```

### 方案二：使用 Let's Encrypt 免费证书（生产环境）

#### 步骤 1：安装 Certbot

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install certbot

# CentOS/RHEL
sudo yum install certbot
```

#### 步骤 2：获取证书

```bash
# 方式1：自动配置（推荐）
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# 方式2：手动配置（需要自己配置 Nginx）
sudo certbot certonly --manual -d yourdomain.com
```

#### 步骤 3：配置 Spring Boot

```yaml
server:
  port: 443
  ssl:
    enabled: true
    key-store: file:/etc/letsencrypt/live/yourdomain.com/keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
```

**注意：** Let's Encrypt 证书是 `.pem` 格式，需要转换为 `.p12` 格式：

```bash
# 转换证书格式
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out /etc/letsencrypt/live/yourdomain.com/keystore.p12 \
  -name tomcat \
  -password pass:your_password
```

### 方案三：使用 Nginx 反向代理（推荐生产环境）

**优势：**
- Nginx 处理 SSL/TLS，性能更好
- Spring Boot 专注业务逻辑
- 便于负载均衡和静态资源服务

**Nginx 配置示例：**

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    # HTTP 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    # SSL 证书配置
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    # SSL 优化配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 反向代理到 Spring Boot
    location / {
        proxy_pass http://localhost:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 三、配置 HTTPS 后的优化

### 1. 移除前端 AES 加密

配置 HTTPS 后，可以移除前端的 AES 加密代码：

```typescript
// 删除或注释掉 crypto.ts 中的加密逻辑
// import { encrypt } from '@/utils/crypto'

// 直接传输明文密码（HTTPS 会自动加密）
password: loginForm.password  // 不再需要 encrypt()
```

### 2. 后端移除 AES 解密

```java
// UserServiceImpl.java
// 移除 AES 解密逻辑
// String plainPassword = AesUtil.decrypt(dto.getPassword());

// 直接使用 BCrypt 验证（密码已经是明文的，因为 HTTPS 已加密传输）
if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
    throw new BusinessException("密码错误");
}
```

### 3. 强制 HTTPS（生产环境）

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 强制 HTTPS（生产环境）
            .requiresChannel(channel -> 
                channel.requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                    .requiresSecure())
            // ... 其他配置
    }
}
```

## 四、测试 HTTPS

### 1. 浏览器访问

```
https://localhost:8443/swagger-ui.html
```

**注意：** 自签名证书会显示"不安全"警告，点击"高级" → "继续访问"即可。

### 2. 使用 curl 测试

```bash
# 跳过证书验证（仅测试用）
curl -k https://localhost:8443/api/auth/login

# 验证证书
curl -v https://localhost:8443/api/auth/login
```

## 五、总结

### HTTPS 的优势

1. ✅ **密钥安全**：密钥在握手时动态生成，不暴露在代码中
2. ✅ **身份认证**：通过 CA 证书验证服务器身份，防止中间人攻击
3. ✅ **数据完整性**：防止传输中被篡改
4. ✅ **标准化**：国际标准，浏览器原生支持
5. ✅ **性能优化**：底层实现，性能开销小

### 前端 AES 加密的问题

1. ❌ **密钥暴露**：密钥硬编码在前端代码中
2. ❌ **无法防中间人**：攻击者可以获取密钥并解密
3. ❌ **增加复杂度**：前后端都需要加解密逻辑
4. ❌ **性能开销**：JavaScript 加密性能较差

### 建议

- **开发环境**：使用自签名证书
- **生产环境**：使用 Let's Encrypt 免费证书或商业证书
- **高并发场景**：使用 Nginx 反向代理处理 SSL/TLS
- **配置 HTTPS 后**：移除前端 AES 加密，简化代码

---

**结论：HTTPS 是传输层加密的标准方案，比应用层加密更安全、更可靠、更高效。**

