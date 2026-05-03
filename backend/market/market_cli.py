import argparse
import json

from market_service import get_market_snapshot


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Market analysis CLI")
    parser.add_argument("--symbol", required=True, help="Token symbol")
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    response = get_market_snapshot(args.symbol)
    print(json.dumps(response, indent=2))


if __name__ == "__main__":
    main()
