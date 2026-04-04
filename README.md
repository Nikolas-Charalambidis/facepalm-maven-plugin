# 🤦‍♂️ Facepalm Maven Plugin

One commit.  
One leaked API key.  
One **$55,444.78 bill**.

That’s not hypothetical. It actually happened to a student who accidentally exposed their Google Cloud credentials publicly:

https://www.reddit.com/r/googlecloud/comments/1noctxi/student_hit_with_a_5544478_google_cloud_bill/

Once a secret hits Git, it’s not “deleted”.  
It’s **forever**. In history, forks, clones, caches, bots.

---

## What this plugin does

**Facepalm** scans your entire repository and stops you before you:

- Commit API keys
- Leak passwords
- Push tokens
- Accidentally publish secrets that cost real money

The goal is simple: stop the mistake before it leaves your machine.

---

## Why you want this

- Secrets leak silently
- Bots scan GitHub constantly
- Abuse starts within minutes
- Cleanup is painful (and often impossible)

This is a guardrail, not a report.

---

## Recommended setup (important)

Add it to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>dev.nichar</groupId>
            <artifactId>facepalm-maven-plugin</artifactId>
            <version>1.0.0</version>
        </plugin>
    </plugins>
</build>
```

To be effective, hook it into a Git pre-commit hook.
Running it only in CI is too late: by then the secret may already be in Git history.

Example `.git/hooks/pre-commit`:

```shell
#!/bin/sh
mvn -q facepalm:scan
RESULT=$?

if [ $RESULT -ne 0 ]; then
echo "Facepalm: potential secret detected. Commit blocked."
exit 1
fi

exit 0
```

Make it executable:

```shell
chmod +x .git/hooks/pre-commit
```

---

## Philosophy

You don’t need another report.  
You need a hard stop before damage happens.  
Zero regret.
