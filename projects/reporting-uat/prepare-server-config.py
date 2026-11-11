#!/usr/bin/env python3
"""Prepare a reporting-only Tomcat overlay from the selected runtime image."""

import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


def prepare(source: Path, destination: Path) -> None:
    if source.resolve() == destination.resolve():
        raise ValueError("Keep the original runtime configuration for rollback")
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))
    tree = ET.parse(source, parser=parser)
    hosts = list(tree.getroot().iter("Host"))
    if len(hosts) != 1 or hosts[0].get("name") != "localhost":
        raise ValueError("Expected the existing single-host OpenELIS runtime")
    host = hosts[0]
    contexts = {(node.get("path", "").rstrip("/"), node.get("docBase")) for node in host.findall("Context")}
    if contexts != {("/api", "ROOT"), ("/api/OpenELIS-Global", "OpenELIS-Global")}:
        raise ValueError("Unexpected application mappings; inspect the runtime before preparing this overlay")
    # OpenELIS declares its API contexts explicitly. Automatic WAR discovery
    # would deploy the same application again under /OpenELIS-Global.
    host.set("autoDeploy", "false")
    host.set("deployOnStartup", "false")
    tree.write(destination, encoding="utf-8", xml_declaration=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="server.xml read from the selected image")
    parser.add_argument("destination", type=Path, help="new release's server.xml")
    args = parser.parse_args()
    prepare(args.source, args.destination)
