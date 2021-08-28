#!/usr/bin/env python3

#
#  Copyright 2020 F5 Networks
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

"""
This script updates a set of Kubernetes manifest YAML files image versions
with a new version.

When invoked, this script requires at least two parameters: 1) a version id
with no spaces, 2) one or more paths to YAML files to update.
"""

import io
import sys
import ruamel.yaml

if len(sys.argv) < 3:
    print("missing parameters <version> <file paths...>", file=sys.stderr)
    sys.exit(1)

VERSION = sys.argv[1]
FILES = sys.argv[2:]


def value_or_none(dictionary: dict, key: str):
    """returns value of key from dictionary otherwise if not found None"""
    return dictionary[key] if key in dictionary else None


def navigate_by_keys(dictionary: dict, subkeys: list):
    """return the value of a sub-dict based on key list, if not found None"""
    subdict = dictionary
    value = None
    for key in subkeys:
        value = value_or_none(subdict, key)
        if value:
            subdict = value

    return value


def update_image_version(name: str, new_version: str):
    """returns the passed image name modified with the specified version"""
    parts = name.rsplit(':', 1)
    return f'{parts[0]}:{new_version}'


def update_yaml_doc_in_place(doc, version: str) -> bool:
    """updates a yaml document in-place to the latest image version"""

    # Navigates to the sub-dictionary via the keys as ordered below
    containers_subkeys = ['spec', 'template', 'spec', 'containers']
    containers = navigate_by_keys(doc, containers_subkeys)

    if not containers:
        return False

    changed = False
    for container in containers:
        if 'image' in container:
            old_name = container['image']
            new_name = update_image_version(old_name, version)
            if container['image'] != new_name:
                container['image'] = new_name
                changed = True

        if 'env' in container:
            for i, pair in enumerate(container['env']):
                is_valid = 'name' in pair and 'value' in pair
                if not is_valid or pair['name'] != 'version':
                    continue
                if container['env'][i]['value'] != version:
                    container['env'][i]['value'] = version
                    changed = True

    return changed


def update_file_in_place(file: str, version: str):
    """update the container image version in a file with the passed version"""
    yaml = ruamel.yaml.YAML()
    yaml.preserve_quotes = True

    buffer = io.StringIO()
    changed = False

    with open(file, mode='r', encoding='utf-8') as stream:
        data = yaml.load_all(stream)
        is_first = True
        for inner_doc in data:
            inner_doc_changed = update_yaml_doc_in_place(inner_doc, version)

            if inner_doc_changed:
                changed = True

            if not is_first:
                print('---', file=buffer)

            yaml.dump(inner_doc, buffer)
            is_first = False

    if changed:
        print(f'{file} updated', file=sys.stderr)
        with open(file, mode='w+', encoding='utf-8') as stream:
            stream.truncate()
            stream.write(buffer.getvalue())
            stream.flush()
    else:
        print(f'{file} not changed', file=sys.stderr)


for f in FILES:
    update_file_in_place(f, VERSION)
