import argparse
import json

from control_service import FEATURES, handle_feature


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Control layer CLI")
    subparsers = parser.add_subparsers(dest="feature", required=True)
    for key in FEATURES.keys():
        subparsers.add_parser(key, help=f"Call {key} feature")
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    response = handle_feature(args.feature)
    print(json.dumps(response, indent=2))


if __name__ == "__main__":
    main()
