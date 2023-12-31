package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@EnableAspectJAutoProxy(exposeProxy = true)
class HmDianPingApplicationTests {


    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es= Executors.newFixedThreadPool(500);
    @Test
    void testIdWorker(){
        CountDownLatch latch=new CountDownLatch(300);
        Runnable task=()->{
            for (int i=0;i<10;++i){
                long id= redisIdWorker.nextId("order");
                System.out.println("id="+id);
            }
            latch.countDown();
        };
        long begin=System.currentTimeMillis();
        for (int i=0;i<300;++i){
            es.submit(task);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long end=System.currentTimeMillis();
        System.out.println("time="+(end-begin));
    }

}
