# Git Hooks

For Facepalm to be effective, it should catch secrets before they are
committed.\
Once a secret enters Git history, removing it is difficult and often
incomplete.

------------------------------------------------------------------------

## Pre-commit Hook (recommended)

Runs on every `git commit`. If Facepalm detects a secret, the commit is
blocked.

Create (or edit):

```sh
.git/hooks/pre-commit
```
```sh
#!/bin/sh

mvn -q facepalm:scan
RESULT=$?

if [ $RESULT -ne 0 ]; then
    echo "Facepalm: potential secret detected. Commit blocked."
    echo "Check the Maven output above for details."
    exit 1
fi

exit 0
```

### Make it executable

```bash
chmod +x .git/hooks/pre-commit
```

------------------------------------------------------------------------

## Pre-push Hook (optional)

Runs on `git push`. Useful as a second line of defense.

Create (or edit):

```sh
.git/hooks/pre-push
```
```sh
#!/bin/sh

mvn -q facepalm:scan
RESULT=$?

if [ $RESULT -ne 0 ]; then
    echo "Facepalm: push blocked due to potential secrets."
    exit 1
fi

exit 0
```

------------------------------------------------------------------------

## Important notes

-   Git hooks are **not shared by default**. Each developer must install
    them locally.

-   For teams, consider committing hooks via a tool (e.g. custom setup
    script, or frameworks like Husky).

-   Hooks can be bypassed with:

    ```bash
    git commit --no-verify
    ```

------------------------------------------------------------------------

## Why use hooks?

-   **Local and immediate** --- secrets never leave your machine\
-   **Prevents history poisoning** --- cleanup requires rewriting Git
    history\
-   **Acts as a safety net** --- catches mistakes early
