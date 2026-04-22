package cn.imhtb.live.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Redisson setup split by profile to avoid local Redis auth mismatches.
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    @Profile("local")
    public RedissonClient localRedissonClient(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.database:0}") int database
    ) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database);
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("!local")
    public RedissonClient redissonClient(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.password:}") String password,
            @Value("${spring.redis.database:0}") int database
    ) {
        try {
            return createClient(host, port, database, password);
        } catch (RedisException exception) {
            String message = exception.getMessage();
            boolean authNotRequired = message != null
                    && (message.contains("Client sent AUTH, but no password is set")
                    || message.contains("ERR Client sent AUTH"));
            if (authNotRequired) {
                return createClient(host, port, database, null);
            }
            throw exception;
        }
    }

    private RedissonClient createClient(String host, int port, int database, String password) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database);

        if (password != null && !password.trim().isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }
}
