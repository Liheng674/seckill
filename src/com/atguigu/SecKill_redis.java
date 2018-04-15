package com.atguigu;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.Transaction;



public class SecKill_redis {
	
	private static final  org.slf4j.Logger logger =LoggerFactory.getLogger(SecKill_redis.class) ;

	public static void main(String[] args) {
 
        JedisPool jedisPool = JedisPoolUtil.getJedisPoolInstance();
		Jedis jedis =new Jedis("192.168.154.148",6379);
		
		System.out.println(jedis.ping());
	 
		jedis.close();
		
  
		
		
			
	}
	
	
	public static boolean doSecKill(String uid,String prodid) throws IOException {
	
		//创建key值
		 JedisPool jedisPool = JedisPoolUtil.getJedisPoolInstance();
		Jedis jedis =jedisPool.getResource();
		String qtKey = "sk:"+prodid+":qt";
		String usrKey = "sk:"+prodid+":usr";
		
		//判断是否已抽中
		if(jedis.sismember(usrKey, uid)) {
			System.err.println("不能重复抢！！");
			jedis.close();
			return false;
		}
		//加锁
		jedis.watch(qtKey);
		
		//判断库存
		String qtStr = jedis.get(qtKey);
		if(qtStr==null) {
			System.err.println("未初始化！！");
			jedis.close();
			return false;
		}
		int qtno = Integer.parseInt(qtStr);
		if(qtno==0) {
			System.err.println("已抢光！！");
			jedis.close();
			return false;
		}
		
		//组队
		Transaction tran = jedis.multi();
		
		//减库存
		tran.decr(qtKey);
		
		//加人
		tran.sadd(usrKey, uid);
		
		List<Object> exec = tran.exec();
		//判断是否成功
		if(exec==null||exec.size()==0) {
			System.err.println("秒杀失败！！");
			jedis.close();
			return false;
		}
		
		jedis.close();
		System.out.println("秒杀成功！！！");
		
		return true;
	}
	

}
