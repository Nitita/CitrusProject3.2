package autotests;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.xml.XmlSuite;

import java.io.*;
import java.util.*;

public class ParallelClassesReporter extends TestListenerAdapter {
    private static class TimeInterval {
        private String name;
        private long intervalMs;

        public TimeInterval(String name, long intervalMs) {
            this.name = name;
            this.intervalMs = intervalMs;
        }

        public String getName() {
            return name;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void extend(long byMs) {
            intervalMs += byMs;
        }
    }

    private final static String CHARSET = "utf-8";
    private Map<Long, Deque<TimeInterval>> threadTasks = new HashMap<>();
    private Map<Long, String> threadNames = new HashMap<>();
    private Map<String, String> fileNameMap = new HashMap<>();
    private int fileNameIndex = 0;


    private synchronized void collectThreadStats(ITestResult testResult) {
        ITestNGMethod testMethod = testResult.getMethod();
        if (testMethod.isBeforeSuiteConfiguration()
                || testMethod.isAfterSuiteConfiguration()
                || testMethod.isBeforeTestConfiguration()
                || testMethod.isAfterTestConfiguration()) {
            return;// ignore global setup and teardown
        }

        Thread currentThread = Thread.currentThread();
        long currentThreadId = currentThread.getId();

        String testClass = testResult.getTestClass().getRealClass().getName();

        long finishTimeMillis = testResult.getEndMillis();
        if (finishTimeMillis == testResult.getStartMillis()) {
            finishTimeMillis = System.currentTimeMillis();
        }
        long duration = finishTimeMillis - testResult.getStartMillis();

        Deque<TimeInterval> threadQueue = threadTasks.get(currentThreadId);

        if (threadQueue == null) {
            threadQueue = new ArrayDeque<>();
            threadTasks.put(currentThreadId, threadQueue);
            threadNames.put(currentThreadId, currentThread.getName());
        }

        TimeInterval lastInterval = threadQueue.peekLast();
        if (lastInterval != null && lastInterval.getName().equals(testClass)) {
            lastInterval.extend(duration);
        } else {
            threadQueue.addLast(new TimeInterval(testClass, duration));
        }
    }


    private synchronized void generateReport(ITestContext context) {
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
                    context.getOutputDirectory().substring(0, context.getOutputDirectory().lastIndexOf(File.separator)) + File.separator + generateFileName(context) + "-parallel.html")), CHARSET));

            writeFileHeader(writer);

            XmlSuite.ParallelMode parallelMode = context.getCurrentXmlTest().getParallel();
            if (parallelMode != XmlSuite.ParallelMode.CLASSES) {
                writeWrongModeMessage(writer, parallelMode);
            } else {
                List<Long> threadIds = new ArrayList<>(threadTasks.keySet());
                Collections.sort(threadIds);

                long maxDuration = 0;

                for (long threadId : threadIds) {
                    long duration = writeThreadInfo(writer, threadId);

                    if (duration > maxDuration) {
                        maxDuration = duration;
                    }
                }

                writeTimeline(writer, maxDuration);

                String suiteClasses = optimizeTasks();

                long optimalDuration = 0;

                for (long threadId : threadIds) {
                    long duration = writeThreadInfo(writer, threadId);

                    if (duration > optimalDuration) {
                        optimalDuration = duration;
                    }
                }

                writer.println("<div>Optimal task allocation savings " + maxDuration / 1000 + "-" + optimalDuration / 1000
                        + "=" + (maxDuration - optimalDuration) / 1000 + "s</div>");
                writer.println("<div>Optimal order classes suite block:</div>");
                writer.println("<pre>");
                writer.println(suiteClasses);
                writer.println("</pre>");
            }

            writeFileFooter(writer);

            writer.flush();
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        resetThreadStats();
    }


    private void resetThreadStats() {
        threadTasks.clear();
        threadNames.clear();
    }


    private String optimizeTasks() {
        Map<Long, Long> threadDurations = new HashMap<>(threadNames.size());
        Set<TimeInterval> allTasks = new TreeSet<>((o1, o2) -> (int) (o2.getIntervalMs() - o1.getIntervalMs()));

        for (long threadId : threadTasks.keySet()) {
            // getting all thread tasks
            TimeInterval task;
            while ((task = threadTasks.get(threadId).pollLast()) != null) {
                allTasks.add(task);
            }

            //resetting thread duration
            threadDurations.put(threadId, 0L);
        }

        StringBuilder suiteClassesBuilder = new StringBuilder();
        suiteClassesBuilder.append("&lt;classes&gt;\n");

        for (TimeInterval longestTask : allTasks) {
            long shortestThreadId = 0;
            long shortestThreadDuration = Long.MAX_VALUE;
            for (long threadId : threadDurations.keySet()) {
                long currentThreadDuration = threadDurations.get(threadId);
                if (currentThreadDuration < shortestThreadDuration) {
                    shortestThreadId = threadId;
                    shortestThreadDuration = currentThreadDuration;
                }
            }
            threadTasks.get(shortestThreadId).addLast(longestTask);
            threadDurations.put(shortestThreadId, shortestThreadDuration + longestTask.getIntervalMs());
            suiteClassesBuilder.append("\t&lt;class name=\"").append(longestTask.getName()).append("\" /&gt;\n");
        }

        suiteClassesBuilder.append("&lt;/classes&gt;");
        return suiteClassesBuilder.toString();
    }


    private String generateFileName(ITestContext context) {
        String fileName;
        String keyToSearch = context.getSuite().getName() + context.getName();
        if (fileNameMap.get(keyToSearch) == null) {
            fileName = context.getName();
        } else {
            fileName = context.getName() + fileNameIndex++;
        }

        fileNameMap.put(keyToSearch, fileName);
        return fileName;
    }


    private void writeFileHeader(PrintWriter writer) {
        writer.println("<html>\n" +
                "<head>\n" +
                "<meta charset=\"" + CHARSET + "\">\n" +
                "<title>Threads & Classes</title>\n" +
                "</head>\n" +
                "<style>\n" +
                ".ruler-timeline,.thread-timeline,.block {white-space: nowrap;}\n" +
                ".header {width: 127px;}\n" +
                ".ruler {width: 120px;height: 25px;}\n" +
                ".ruler:nth-child(even) {background: #F8F8F8;}\n" +
                ".ruler:nth-child(odd) {background: #E0E0E0;}\n" +
                "\n" +
                ".thread { \n" +
                "\twidth: 125px;\n" +
                "\theight: 23px;\n" +
                "\tborder: black;\n" +
                "    border-style: solid;\n" +
                "    border-width: 1px;\n" +
                "}\n" +
                "\n" +
                ".block {\n" +
                "\tdisplay: inline-block;\n" +
                "\toverflow: hidden;\n" +
                "    word-break: break-all;\n" +
                "}\n" +
                "\n" +
                ".no-border {height: 25px;}\n" +
                ".thread-timeline .no-border:nth-child(even) {background: #FCC;}\n" +
                ".thread-timeline .no-border:nth-child(odd) {background: #CFC;}\n" +
                "</style>\n" +
                "<body>\n" +
                "<h2>Threads & Classes</h2>");
    }


    private void writeFileFooter(PrintWriter writer) {
        writer.println("</body>\n</html>");
    }


    private void writeWrongModeMessage(PrintWriter writer, XmlSuite.ParallelMode mode) {
        writer.println("<p>This reporter supports only test run parallel mode \"classes\", but current mode is " + mode + "</p>");
    }


    private long writeThreadInfo(PrintWriter writer, long threadId) {
        writer.println("<div class=\"thread-timeline\">\n\t");

        String threadName = threadNames.get(threadId);
        writer.print("<span class=\"thread block\" title=\"" + threadName + " (" + threadId + ")\" >" + threadName + "</span>");

        long totalDurationMs = 0;
        Deque<TimeInterval> tasks = threadTasks.get(threadId);
        for (TimeInterval task : tasks) {
            String taskName = task.getName();
            String shortTaskName = taskName.substring(taskName.lastIndexOf('.') + 1);
            int durationPixels = (int) (task.getIntervalMs() * 2 / 1000);
            totalDurationMs += task.getIntervalMs();
            writer.print("<span class=\"block no-border\" title=\"" + taskName + " (" + task.getIntervalMs()
                    + "ms)\" style=\"width:" + durationPixels + "px;\">" + shortTaskName + "</span>");
        }

        writer.println("\n</div>");

        return totalDurationMs;
    }


    private void writeTimeline(PrintWriter writer, long durationMs) {
        int timelineMinutes = (int) Math.ceil(durationMs / 1000.0 / 60.0);

        writer.print("<div class=\"ruler-timeline\">\n" +
                "\t<span class=\"block header\">&nbsp;</span>");

        for (int i = 0; i < timelineMinutes; i++) {
            writer.print("<span class=\"block ruler\">" + i * 60 + "s</span>");
        }

        writer.println("\n</div>");
    }


    @Override
    public void onTestFailure(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onTestSkipped(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onTestSuccess(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onConfigurationFailure(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onConfigurationSkip(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onConfigurationSuccess(ITestResult testResult) {
        collectThreadStats(testResult);
    }

    @Override
    public void onFinish(ITestContext context) {
        generateReport(context);
    }
}
