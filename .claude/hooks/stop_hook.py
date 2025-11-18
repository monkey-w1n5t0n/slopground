#!/usr/bin/env python3
"""
Stop Hook for Claude Code

This hook intercepts Claude's stop decisions and prompts the model to continue
with additional context or next steps.

The hook blocks the stop event once, prompting Claude to continue, but allows
subsequent stops to prevent infinite loops.
"""

import json
import os
import sys


def main():
    # Check if this is a recursive call (stop hook already active)
    stop_hook_active = os.environ.get("stop_hook_active", "false").lower() == "true"

    # Read input arguments (conversation context)
    try:
        input_data = sys.stdin.read()
    except Exception:
        input_data = ""

    # If the stop hook is already active, allow the stop to proceed
    if stop_hook_active:
        result = {
            "decision": "approve"
        }
    else:
        # Block the stop and prompt Claude to continue
        result = {
            "decision": "block",
            "reason": (
                "Before stopping, please:\n"
                "1. Verify all tasks are complete\n"
                "2. Summarize what you've accomplished\n"
                "3. Mention any remaining work or next steps\n"
                "4. If everything is done, confirm completion"
            )
        }

    # Output the decision as JSON
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    sys.exit(main())
