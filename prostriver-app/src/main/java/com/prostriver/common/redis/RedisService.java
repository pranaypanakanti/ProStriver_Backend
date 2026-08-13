package com.prostriver.common.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    public <T> T get(String key, TypeReference<T> typeReference) {
        try{
            Object o = redisTemplate.opsForValue().get(key);
            if(o == null) return null;
            return mapper.readValue(o.toString(), typeReference);
        }catch (Exception e) {
            log.error("Exception in Redis get method: ", e);
            return null;
        }
    }

    public void set(String key, Object o, Long ttl) {
        try{
            String value = mapper.writeValueAsString(o);
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MINUTES);
        }catch (Exception e) {
            log.error("Exception in Redis set method: ", e);
        }
    }

    public void delete(String key) {
        try{
            redisTemplate.delete(key);
        }catch (Exception e) {
            log.error("Exception in Redis delete method: ", e);
        }
    }
}
