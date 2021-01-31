package com.diginex.matchingEngine.util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import org.apache.log4j.Logger;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;


public class GcLogUtil {
	private static Logger log = Logger.getLogger(GcLogUtil.class);
	private static int gcMajor=0;
	private static int gcMinor=0;
    static public void startLoggingGc() {
        // http://www.programcreek.com/java-api-examples/index.php?class=javax.management.MBeanServerConnection&method=addNotificationListener
        // https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/GarbageCollectionNotificationInfo.html#GARBAGE_COLLECTION_NOTIFICATION
        for (GarbageCollectorMXBean gcMbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            try {
                ManagementFactory.getPlatformMBeanServer().
                        addNotificationListener(gcMbean.getObjectName(), listener, null,null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static private NotificationListener listener = new NotificationListener() {
        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))  {
            		// https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/GarbageCollectionNotificationInfo.html
                CompositeData cd = (CompositeData) notification.getUserData();
                GarbageCollectionNotificationInfo gcNotificationInfo = GarbageCollectionNotificationInfo.from(cd);
                GcInfo gcInfo = gcNotificationInfo.getGcInfo();
                if(gcNotificationInfo.getGcAction().equalsIgnoreCase("Major")) {
                	++gcMajor;
                } else {
                	++gcMinor;
                }
                log.error("GarbageCollection: "+
                        gcNotificationInfo.getGcAction() + " " +
                        gcNotificationInfo.getGcName() +
                        " duration: " + gcInfo.getDuration() + "ms" +
                        " used: " + sumUsedMb(gcInfo.getMemoryUsageBeforeGc()) + "MB" +
                        " -> " + sumUsedMb(gcInfo.getMemoryUsageAfterGc()) + "MB");
            }
        }
    };

    static private long sumUsedMb(Map<String, MemoryUsage> memUsages) {
        long sum = 0;
        for (MemoryUsage memoryUsage : memUsages.values()) {
            sum += memoryUsage.getUsed();
        }
        return sum / (1024 * 1024);
    }
    
    public static int getMajorGc() {
    	return gcMajor;
    }
    
    public static int getMinorGc() {
    	return gcMinor;
    }
    
    public static void reset() {
    	gcMajor=0;
    	gcMinor=0;
    }
}