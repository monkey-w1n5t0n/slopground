# slopground

A repository demonstrating Claude Code hooks, specifically a Stop hook that prompts Claude to continue and provide additional context before stopping.

## Stop Hook

This repository includes a Stop hook that intercepts Claude's stop decisions and prompts the model to:
1. Verify all tasks are complete
2. Summarize what has been accomplished
3. Mention any remaining work or next steps
4. Confirm completion before stopping

### How It Works

The stop hook is configured in `.claude/settings.json` and uses a Python script (`.claude/hooks/stop_hook.py`) that:

- **First stop attempt**: Blocks the stop and prompts Claude to provide more context
- **Second stop attempt**: Allows the stop to proceed (prevents infinite loops)

### Files

- `.claude/settings.json` - Hook configuration
- `.claude/hooks/stop_hook.py` - Stop hook implementation

### Usage

The hook is automatically activated when Claude Code runs in this repository. When Claude attempts to stop:

1. The hook intercepts the stop event
2. Claude receives a prompt to continue with additional context
3. After Claude responds, a second stop is allowed to proceed

This ensures Claude provides comprehensive responses and doesn't stop prematurely.