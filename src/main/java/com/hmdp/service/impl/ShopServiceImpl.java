package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryById(Long id) {
        //
        Shop shop=queryWithMutex(id);

        return Result.ok(shop);

    }

    //互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id)
    {
        System.out.println("get shop1");

        String key=CACHE_SHOP_KEY+id;
        //1.redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson))
        {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //命中的是否是空值
        if(shopJson!=null) return null;
        //3.实现缓存重建
        //3.1获取互斥锁
        //3.2判断是否获取成功
        //3.3 休眠并重试
        //3.4成功 查数据库
        String lockKey="lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            while (!isLock) {
                Thread.sleep(50);

                shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

                //2.判断是否存在
                if(StrUtil.isNotBlank(shopJson))
                {
                    shop = JSONUtil.toBean(shopJson, Shop.class);
                    return shop;
                }
                //命中的是否是空值
                if(shopJson!=null) return null;

                isLock = tryLock(lockKey);
            }

            //4.查询数据库
            shop = getById(id);
            //5.不存在
            if(shop==null){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //6.存在，写入reids
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

            System.out.println("get shop2");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }

        //7.返回
        return shop;
    }

    private void saveShop2Redis(Long id){
        Shop shop = getById(id);
        
    }

    //缓存穿透
    public Shop queryWithPassThrough(Long id)
    {
        System.out.println("get shop1");

        String key=CACHE_SHOP_KEY+id;
        //1.redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson))
        {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //命中的是否是空值
        if(shopJson!=null) return null;
        //3.存在

        //4.查询数据库
        Shop shop = getById(id);
        //5.不存在
        if(shop==null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        //6.存在，写入reids
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        System.out.println("get shop2");
        //7.返回
        return shop;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //false null->false
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key)
    {
        stringRedisTemplate.delete(key);
    }
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id=shop.getId();
        if(id==null) return Result.fail("店铺id不能为空");
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);

        return Result.ok();
    }


}
