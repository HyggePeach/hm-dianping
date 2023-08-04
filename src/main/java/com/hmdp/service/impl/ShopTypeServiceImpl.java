package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {

        //1.redis查询商铺缓存
        String listJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+"list");

        //2.判断是否存在
        if(StrUtil.isNotBlank(listJson))
        {
            List<ShopType> list=JSONUtil.toList(listJson,ShopType.class);
            return Result.ok(list);
        }

        //4.查询数据库
        List<ShopType> list=query().orderByAsc("sort").list();
        //5.不存在
        if(list==null) return Result.fail("无商品类别");
        //6.存在，写入reids
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+"list",JSONUtil.toJsonStr(list));
        //7.返回
        return Result.ok(list);
    }
}
