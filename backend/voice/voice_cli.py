import argparse
import json

from voice_service import synthesize


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Voice layer CLI")
    parser.add_argument("--text", required=True, help="Text to synthesize")
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    response = synthesize(args.text)
    print(json.dumps(response, indent=2))


if __name__ == "__main__":
    main()
