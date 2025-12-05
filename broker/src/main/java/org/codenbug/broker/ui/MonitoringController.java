package org.codenbug.broker.ui;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

  private final ServletWebServerApplicationContext applicationContext;

  public MonitoringController(ServletWebServerApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * 스레드 풀 상태 모니터링 (JMX 방식)
   */
  @GetMapping("/threadpool")
  public ResponseEntity<Map<String, Object>> getThreadPoolStatus() {
    Map<String, Object> status = new HashMap<>();

    try {
      // JVM 전체 스레드 정보
      ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
      status.put("totalThreadCount", threadMXBean.getThreadCount());
      status.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
      status.put("peakThreadCount", threadMXBean.getPeakThreadCount());

      // JMX를 통한 Tomcat 스레드 풀 정보
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();

      // Tomcat thread pool MBeans 찾기
      Set<ObjectName> threadPoolNames =
          server.queryNames(new ObjectName("Tomcat:type=ThreadPool,name=*"), null);
      Map<String, Object> tomcatThreadPools = new HashMap<>();

      for (ObjectName threadPoolName : threadPoolNames) {
        Map<String, Object> poolInfo = new HashMap<>();

        try {
          // 스레드 풀 정보 가져오기
          Object currentThreadCount = server.getAttribute(threadPoolName, "currentThreadCount");
          Object currentThreadsBusy = server.getAttribute(threadPoolName, "currentThreadsBusy");
          Object maxThreads = server.getAttribute(threadPoolName, "maxThreads");
          Object minSpareThreads = server.getAttribute(threadPoolName, "minSpareThreads");
          Object connectionCount = server.getAttribute(threadPoolName, "connectionCount");
          Object maxConnections = server.getAttribute(threadPoolName, "maxConnections");
          Object acceptCount = server.getAttribute(threadPoolName, "acceptCount");

          poolInfo.put("currentThreadCount", currentThreadCount);
          poolInfo.put("currentThreadsBusy", currentThreadsBusy);
          poolInfo.put("maxThreads", maxThreads);
          poolInfo.put("minSpareThreads", minSpareThreads);
          poolInfo.put("connectionCount", connectionCount);
          poolInfo.put("maxConnections", maxConnections);
          poolInfo.put("acceptCount", acceptCount);

          // 사용률 계산
          if (currentThreadsBusy != null && maxThreads != null) {
            int busy = ((Number) currentThreadsBusy).intValue();
            int max = ((Number) maxThreads).intValue();
            if (max > 0) {
              double utilization = (double) busy / max * 100;
              poolInfo.put("utilizationPercentage", Math.round(utilization * 100.0) / 100.0);
            }
          }

        } catch (Exception e) {
          poolInfo.put("error", "Failed to get attributes: " + e.getMessage());
        }

        tomcatThreadPools.put(threadPoolName.getKeyProperty("name"), poolInfo);
      }

      status.put("tomcatThreadPools", tomcatThreadPools);

      // Executor MBeans도 확인
      Set<ObjectName> executorNames =
          server.queryNames(new ObjectName("Tomcat:type=Executor,name=*"), null);
      Map<String, Object> executors = new HashMap<>();

      for (ObjectName executorName : executorNames) {
        Map<String, Object> executorInfo = new HashMap<>();

        try {
          Object activeCount = server.getAttribute(executorName, "activeCount");
          Object poolSize = server.getAttribute(executorName, "poolSize");
          Object corePoolSize = server.getAttribute(executorName, "corePoolSize");
          Object maximumPoolSize = server.getAttribute(executorName, "maximumPoolSize");
          Object queueSize = server.getAttribute(executorName, "queueSize");

          executorInfo.put("activeCount", activeCount);
          executorInfo.put("poolSize", poolSize);
          executorInfo.put("corePoolSize", corePoolSize);
          executorInfo.put("maximumPoolSize", maximumPoolSize);
          executorInfo.put("queueSize", queueSize);

        } catch (Exception e) {
          executorInfo.put("error", "Failed to get attributes: " + e.getMessage());
        }

        executors.put(executorName.getKeyProperty("name"), executorInfo);
      }

      if (!executors.isEmpty()) {
        status.put("tomcatExecutors", executors);
      }

      // 메모리 정보
      Runtime runtime = Runtime.getRuntime();
      Map<String, Object> memoryInfo = new HashMap<>();
      memoryInfo.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
      memoryInfo.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
      memoryInfo.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
      memoryInfo.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
      status.put("memory", memoryInfo);

      status.put("timestamp", System.currentTimeMillis());
      status.put("status", "success");

    } catch (Exception e) {
      status.put("status", "error");
      status.put("error", e.getMessage());
      e.printStackTrace();
    }

    return ResponseEntity.ok(status);
  }

  /**
   * 간단한 스레드 풀 요약 정보 (JMX 방식)
   */
  @GetMapping("/threadpool/summary")
  public ResponseEntity<Map<String, Object>> getThreadPoolSummary() {
    Map<String, Object> summary = new HashMap<>();

    try {
      ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
      summary.put("totalJvmThreads", threadMXBean.getThreadCount());

      // JMX를 통한 Tomcat 스레드 풀 정보
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();

      // 첫 번째 ThreadPool MBean 사용 (보통 main HTTP connector)
      Set<ObjectName> threadPoolNames =
          server.queryNames(new ObjectName("Tomcat:type=ThreadPool,name=*"), null);

      boolean foundThreadPool = false;

      for (ObjectName threadPoolName : threadPoolNames) {
        try {
          Object currentThreadsBusy = server.getAttribute(threadPoolName, "currentThreadsBusy");
          Object maxThreads = server.getAttribute(threadPoolName, "maxThreads");
          Object currentThreadCount = server.getAttribute(threadPoolName, "currentThreadCount");
          Object connectionCount = server.getAttribute(threadPoolName, "connectionCount");

          if (currentThreadsBusy != null && maxThreads != null) {
            int busy = ((Number) currentThreadsBusy).intValue();
            int max = ((Number) maxThreads).intValue();
            int current = currentThreadCount != null ? ((Number) currentThreadCount).intValue() : 0;
            int connections = connectionCount != null ? ((Number) connectionCount).intValue() : 0;

            summary.put("activeThreads", busy);
            summary.put("maxThreads", max);
            summary.put("poolSize", current);
            summary.put("queueSize", 0); // ThreadPool에서는 직접 큐 정보 없음
            summary.put("connectionCount", connections);

            // 사용률 계산
            double utilization = max > 0 ? (double) busy / max * 100 : 0;
            summary.put("utilization", Math.round(utilization * 100.0) / 100.0);

            // 상태 판단
            String status;
            if (utilization < 50) {
              status = "HEALTHY";
            } else if (utilization < 80) {
              status = "BUSY";
            } else {
              status = "OVERLOADED";
            }
            summary.put("status", status);
            summary.put("threadPoolName", threadPoolName.getKeyProperty("name"));

            foundThreadPool = true;
            break; // 첫 번째 스레드 풀 정보만 사용
          }
        } catch (Exception e) {
          // 다음 스레드 풀 시도
          continue;
        }
      }

      // ThreadPool에서 정보를 못 가져왔으면 Executor 시도
      if (!foundThreadPool) {
        Set<ObjectName> executorNames =
            server.queryNames(new ObjectName("Tomcat:type=Executor,name=*"), null);

        for (ObjectName executorName : executorNames) {
          try {
            Object activeCount = server.getAttribute(executorName, "activeCount");
            Object maximumPoolSize = server.getAttribute(executorName, "maximumPoolSize");
            Object poolSize = server.getAttribute(executorName, "poolSize");
            Object queueSize = server.getAttribute(executorName, "queueSize");

            if (activeCount != null && maximumPoolSize != null) {
              int active = ((Number) activeCount).intValue();
              int max = ((Number) maximumPoolSize).intValue();
              int current = poolSize != null ? ((Number) poolSize).intValue() : 0;
              int queue = queueSize != null ? ((Number) queueSize).intValue() : 0;

              summary.put("activeThreads", active);
              summary.put("maxThreads", max);
              summary.put("poolSize", current);
              summary.put("queueSize", queue);

              double utilization = max > 0 ? (double) active / max * 100 : 0;
              summary.put("utilization", Math.round(utilization * 100.0) / 100.0);

              String status =
                  utilization < 50 ? "HEALTHY" : utilization < 80 ? "BUSY" : "OVERLOADED";
              summary.put("status", status);
              summary.put("executorName", executorName.getKeyProperty("name"));

              foundThreadPool = true;
              break;
            }
          } catch (Exception e) {
            continue;
          }
        }
      }

      if (!foundThreadPool) {
        summary.put("status", "NO_THREADPOOL_FOUND");
        summary.put("activeThreads", 0);
        summary.put("maxThreads", 0);
        summary.put("poolSize", 0);
        summary.put("queueSize", 0);
        summary.put("utilization", 0.0);
      }

      summary.put("timestamp", System.currentTimeMillis());

    } catch (Exception e) {
      summary.put("status", "ERROR");
      summary.put("error", e.getMessage());
      summary.put("activeThreads", 0);
      summary.put("maxThreads", 0);
      summary.put("poolSize", 0);
      summary.put("queueSize", 0);
      summary.put("utilization", 0.0);
    }

    return ResponseEntity.ok(summary);
  }
}
