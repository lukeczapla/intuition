from pathlib import Path
from sys import argv

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_text_splitters import RecursiveCharacterTextSplitter

BODY_FILE = f"python/body-{argv[1]}.txt"
QUESTION_FILE = f"python/question-{argv[1]}.txt"

CHROMA_DIR = "./chroma_db"


def main():
    body_text = Path(BODY_FILE).read_text(encoding="utf-8")
    question = Path(QUESTION_FILE).read_text(encoding="utf-8").strip()

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=1200,
        chunk_overlap=200,
    )

    docs = [Document(page_content=body_text)]
    chunks = splitter.split_documents(docs)

    embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

    db = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        persist_directory=CHROMA_DIR,
        collection_name="body_text",
    )

    retrieved_docs = db.similarity_search(
        question,
        k=5,
    )

    context = "\n\n---\n\n".join(
        doc.page_content for doc in retrieved_docs
    )

    llm = ChatOpenAI(
        model="gpt-4.1-mini",
        temperature=0,
    )

    response = llm.invoke(
        f"""Answer the question using only the context below.

If the answer is not in the context, say you do not know.

Context:
{context}

Question:
{question}
"""
    )

    print(response.content)


if __name__ == "__main__":
    main()
