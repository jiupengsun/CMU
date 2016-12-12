import redis
import pickle
from webapps import settings
from Helper.Constant import *

_redis = redis.StrictRedis(host=settings.REDIS_HOST, port=settings.REDIS_PORT, db=0)

def putRedis(key, value):
  _redis.set(key, value)

def getRedis(key):
  return _redis.get(key)

def removeRedis(key):
  return _redis.delete(key)

#reference : http://stackoverflow.com/questions/15219858/how-to-store-a-complex-object-in-redis-using-redis-py
def addUserToChannel(userid, channel):
  dict = _redis.hmget()
  packed_obj = pickle.dumps(channel)
  dict['userid'] = packed_obj
  _redis.hmset(USER_CHANNEL_DICT, dict)

def getUserChannel(userid):
  dict = _redis.hmget(USER_CHANNEL_DICT)
  obj = dict.get(userid)
  # the key does not exist in the redis
  if obj is None:
    return None
  else:
    unpacked_obj = pickle.loads(obj)
    return unpacked_obj

def deleteUserandChannel(userid):
  return _redis.scard(userid)

def incChannelCount(channel_id):
  _redis.hincrby(channel_id, 1)

def decChannelCount(channel_id):
  _redis.decr(channel_id, 1)

def addChannelToDoc(docid, channel_name):
  return _redis.sadd(docid, channel_name)

def getSizeofChannel(docid):
  return _redis.scard(docid)

