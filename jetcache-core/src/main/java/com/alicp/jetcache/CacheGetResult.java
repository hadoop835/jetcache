/**
 * Created on  13-09-09 18:16
 */
package com.alicp.jetcache;

/**
 * @author <a href="mailto:yeli.hl@taobao.com">huangli</a>
 */
public class CacheGetResult<V> extends CacheResult {
    private V value;

    public static final CacheGetResult NOT_EXISTS_WITHOUT_MSG = new CacheGetResult(CacheResultCode.NOT_EXISTS, null, null);
    public static final CacheGetResult EXPIRED_WITHOUT_MSG = new CacheGetResult(CacheResultCode.EXPIRED, null ,null);

    public CacheGetResult(CacheResultCode resultCode, String message, V value) {
        super(resultCode, message);
        this.value = value;
    }

    public CacheGetResult(Throwable ex) {
        super(ex);
    }


    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }


}
