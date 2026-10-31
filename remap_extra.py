#!/usr/bin/env python3
"""Dopelnia przemapowanie zrodel: zamienia pozostale field_N / method_N / comp_N na nazwy Yarn.
Pobiera yarn-<wersja>-v2.jar (wersja z build.gradle) i czyta z niego mappings/mappings.tiny."""
import io, os, re, sys, urllib.request, zipfile

def yarn_version():
    for line in open('build.gradle', encoding='utf8'):
        m = re.search(r'net\.fabricmc:yarn:([^:"]+):v2', line)
        if m and '$' not in m.group(1):
            return m.group(1)
    sys.exit('Nie znaleziono wersji Yarn w build.gradle')

def load(v):
    url = f'https://maven.fabricmc.net/net/fabricmc/yarn/{v}/yarn-{v}-v2.jar'
    data = urllib.request.urlopen(url, timeout=120).read()
    z = zipfile.ZipFile(io.BytesIO(data))
    return z.read('mappings/mappings.tiny').decode('utf8').split('\n')

def parse(lines):
    names = {}
    for ln in lines[1:]:
        t = ln.lstrip('\t').split('\t')
        if ln.startswith('\t') and not ln.startswith('\t\t') and t[0] in ('f', 'm') and len(t) >= 4:
            inter, named = t[2], t[3]
            if named and named != inter and re.fullmatch(r'(field|method|comp)_\d+', inter):
                names[inter] = named
    return names

def run(names, root='src/main/java'):
    pat = re.compile(r'\b(?:field|method|comp)_\d+\b')
    changed = 0
    for dp, _, fs in os.walk(root):
        for f in fs:
            if not f.endswith('.java'):
                continue
            p = os.path.join(dp, f)
            s = open(p, encoding='utf8').read()
            s2 = pat.sub(lambda m: names.get(m.group(0), m.group(0)), s)
            if s2 != s:
                open(p, 'w', encoding='utf8').write(s2)
                changed += 1
    return changed

if __name__ == '__main__':
    v = yarn_version()
    names = parse(load(v))
    print(f'remap_extra: Yarn {v}, nazw: {len(names)}, zmienionych plikow: {run(names)}')
