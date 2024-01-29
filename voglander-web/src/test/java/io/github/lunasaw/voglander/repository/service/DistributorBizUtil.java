//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.github.lunasaw.voglander.repository.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class DistributorBizUtil {
    public static final BigInteger MAX_ITEM_ID = BigInteger.valueOf(1099511627775L);
    private static final int       SHIFT_BITS  = 40;

    public DistributorBizUtil() {}

    public static String processDistributeItemId(String itemId) {
        if (StringUtils.isBlank(itemId)) {
            return itemId;
        } else {
            return isDistributeItem(itemId) ? String.valueOf(getSupplyItemId(itemId)) : itemId;
        }
    }

    public static List<String> processDistributeItemIds(List<String> itemIds) {
        if (itemIds != null && itemIds.size() != 0) {
            List<String> returnItemIds = new ArrayList();
            Iterator var2 = itemIds.iterator();

            while (var2.hasNext()) {
                String itemId = (String)var2.next();
                if (isDistributeItem(itemId)) {
                    returnItemIds.add(getSupplyItemId(itemId) + "");
                } else {
                    returnItemIds.add(itemId);
                }
            }

            return returnItemIds;
        } else {
            return itemIds;
        }
    }

    public static List<String> filterDistributeItemIds(List<String> itemIds) {
        if (itemIds != null && itemIds.size() != 0) {
            List<String> returnItemIds = new ArrayList();
            Iterator var2 = itemIds.iterator();

            while (var2.hasNext()) {
                String itemId = (String)var2.next();
                if (!isDistributeItem(itemId)) {
                    returnItemIds.add(itemId);
                }
            }

            return returnItemIds;
        } else {
            return itemIds;
        }
    }

    public static String getDistributeKey(String vItemId) {
        return "fx_discount_" + vItemId;
    }

    public static boolean isDistributeItem(String vItemId) {
        BigInteger vItemIntId = new BigInteger(vItemId);
        return vItemIntId.compareTo(MAX_ITEM_ID) > 0;
    }

    public static Long getSupplyItemId(String vItemId) {
        BigInteger vItemIntId = new BigInteger(vItemId);
        return vItemIntId.and(MAX_ITEM_ID).longValue();
    }

    public static Long getDistributorId(String vItemId) {
        BigInteger vItemIntId = new BigInteger(vItemId);
        return vItemIntId.shiftRight(40).longValue();
    }

    public static BigInteger generate(long retailSellerId, long supplyItemId) {
        return BigInteger.valueOf(retailSellerId).shiftLeft(40).add(BigInteger.valueOf(supplyItemId));
    }

    public static void main(String[] args) {
        System.out.println(getSupplyItemId("123"));
        System.out.println(getDistributorId("1357384984866313858068"));
    }
}
