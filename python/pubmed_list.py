import requests
import xml.etree.ElementTree as ET
import sys, os
from utils import grouper
from math import ceil

def ask(query):
    pub_ids = get_pub_ids(query)

    pubs = []
    chunk_size = 200
    step_size = ceil(len(pub_ids)/chunk_size)

    for i, g in enumerate(grouper(pub_ids, chunk_size)):
        print(i, step_size)
        get_pub_content([id for id in g if id], i == step_size-1)

    return pubs


def get_pub_ids(query):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term={query}&retmax=25000&sort=relevance"
    )

    res.raise_for_status()

    return [f.text for f in ET.fromstring(res.content).find('IdList')]

def tree(root):
    for child in root:
      tree(child)
      print(child.tag, child.attrib, child.text)

def get_pub_content(ids, done=False):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={','.join(ids)}&rettype=text"
    )

    file1 = open("myfile.xml", "wb")
    file1.write(res.content)
    file1.close()
    if os.path.exists("pubmed_list.xml"):
        if done:
            os.system("gtail -n +4 myfile.xml >> pubmed_list.xml")
        else:
            os.system("gtail -n +4 myfile.xml| ghead -n -2 >> pubmed_list.xml")
    else:
        if done:
            os.system("mv myfile.xml pubmed_list.xml")
        else:
            os.system("ghead -n -1 myfile.xml > pubmed_list.xml")

def get_str_id_content(idstr):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={idstr}&rettype=text"
    )

    file1 = open("pubmed_list.xml", "wb")
    file1.write(res.content)
    file1.close()

if __name__ == '__main__':
    if os.path.exists("pubmed_list.xml"):
        os.remove("pubmed_list.xml")
    if len(sys.argv) > 1:
        get_str_id_content(sys.argv[1])
    else:
        print('Provide one argument (list of pmid like "20000,100003,234038")')

