import argparse
import json

from program_service import handle_command


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Program layer CLI")
    parser.add_argument("--text", required=True, help="Command text")
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    response = handle_command(args.text)
    print(json.dumps(response, indent=2))


if __name__ == "__main__":
    main()
