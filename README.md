# Reproduction of logback issue

> tldr; an unsuccessful delete , following a compress, causes disk trashing and cpu at 100% in Logback 

## What is this?

We experienced that a server was rendered fully unusable when
switching a certain appender to use `FixedWindowRollingPolicy`.
A single cpu core was 100% allocated to `logger.debug()` calls. 

This repository, with its instructions, is a reproduction of how to recreate this situation.

![Skjermbilde 2021-06-28 133614](https://user-images.githubusercontent.com/618076/123632919-ccb28b80-d818-11eb-862f-af535f175906.png)
![Skjermbilde 2021-06-28 133532](https://user-images.githubusercontent.com/618076/123632920-cd4b2200-d818-11eb-8765-a3b75df2c188.png)
![Skjermbilde 2021-06-28 133411](https://user-images.githubusercontent.com/618076/123632923-cd4b2200-d818-11eb-89fc-8193782103b8.png)
![Skjermbilde 2021-06-28 133821](https://user-images.githubusercontent.com/618076/123632877-bc9aac00-d818-11eb-91c5-795f77e2a2d8.png)

## So what was going on?

The config looked like this:
```
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>test.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
        <fileNamePattern>test.%i.log.zip</fileNamePattern>
        <minIndex>1</minIndex>
        <maxIndex>20</maxIndex>
    </rollingPolicy>

    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
        <maxFileSize>100MB</maxFileSize>
    </triggeringPolicy>

    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{35} - %msg%n</pattern>
    </encoder>
</appender>
```

When the problem arose, every log entry resulted in 20 files
being moved/renamed. _Each one of those only had a single line
of difference from the previous._

The one thing that stood out was a single inconspicious line:

```
Could not delete [debugging.log]
```

After inspecting the code behind the `FixedWindowRollingPolicy`
I found this
```java
            case ZIP:
                compressor.compress(getActiveFileName(), fileNamePattern.convertInt(minIndex), zipEntryFileNamePattern.convert(new Date()));
                break;
```
end up calling this code in `Compressor`:
```java
    if (!file2zip.delete()) {
        addStatus(new WarnStatus("Could not delete [" + nameOfFile2zip + "].", this));
    }
```

This means that _if the log file is not deleted, it remains as big and will of course 
trigger a new rollover on the next triggering event_, resulting in the just created
zip file to be moved (and every other in turn), zipping all 100MB+one line all over again.

The old `File#delete` API does not say why it cannot delete the file, but I suspect something
keeps a lock on the file somehow, preventing its deletion.

I am thinking a potentially safer approach is to simply truncate it:
```java
FileWriter fw = new FileWriter(file, false);
fw.flush();
```

In order to test this fix, I would need to verify the issue is fixed, and for that I need a reproduction case,
hence this repository.

## How to reproduce
0. Install the log reading program "glogg"
1. Start this program (easiest: open in intellij->Run)
2. Open the file "test.log" in "glogg.exe"
3. See the cpu spike to a 100% and a lot of output about not being able to delete a file

# Related
- [StackOverflow](https://stackoverflow.com/q/40085486/200987) Logback on windows doesn't close rotated logfiles
- [LOGBACK-879](https://jira.qos.ch/browse/LOGBACK-879) Logback zip compression cannot delete the original files after the zip file is created
- https://stackoverflow.com/a/9800175/200987
- [Logback renamingError](http://logback.qos.ch/codes.html#renamingError)
