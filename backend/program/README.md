# Program Layer (CLI)

This folder provides a simple orchestration layer that accepts a text command
and routes it to the control layer. It can be tested from the command line
before any frontend integration.

## Usage

```bash
cd backend/program
python3 program_cli.py --text "Show my SOL balance"
python3 program_cli.py --text "Market performance for SOL"
```
