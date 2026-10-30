
from pathlib import Path

data_dir = Path("data")

query_file = data_dir / "cran.qry"
corrected_qrels_file = data_dir / "QRelsCorrectedforTRECeval"
output_file = data_dir / "cranqrel_trec.txt"

# Read original query IDs in their dataset order
query_ids = []

with open(query_file, encoding="utf-8") as f:
    for line in f:
        if line.startswith(".I"):
            query_ids.append(str(int(line.split()[1])))

# Read corrected QRELs and map their sequential IDs
# to the original query IDs
count = 0

with open(corrected_qrels_file, encoding="utf-8") as source, \
     open(output_file, "w", encoding="utf-8") as output:

    for line in source:
        parts = line.split()

        if len(parts) != 4:
            continue

        qid, iteration, doc_id, grade = parts

        # Corrected QREL IDs are sequential positions
        original_qid = query_ids[int(qid) - 1]

        # Grades 1-4 are relevant; grade 5 is not relevant
        relevance = 0 if int(grade) == 5 else 1

        output.write(
            f"{original_qid} {iteration} {doc_id} {relevance}\n"
        )

        count += 1

print(f"Queries mapped: {len(query_ids)}")
print(f"Judgements written: {count}")
print(f"Output file: {output_file}")

