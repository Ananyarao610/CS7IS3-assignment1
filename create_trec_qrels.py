from pathlib import Path

# Read the original query IDs from cran.qry, in order.
query_ids = []

with open("data/cran.qry", "r") as f:
    for line in f:
        parts = line.split()

        if len(parts) >= 2 and parts[0] == ".I":
            original_id = str(int(parts[1]))
            query_ids.append(original_id)

# Map sequential qrels IDs to original Cranfield query IDs.
# For example: qrels ID 3 maps to original query ID 4.
query_mapping = {
    str(position): original_id
    for position, original_id in enumerate(query_ids, start=1)
}

# Convert the original three-column qrels into TREC's four-column format.
with open("data/cranqrel", "r") as source, \
     open("data/cranqrel_trec.txt", "w") as output:

    count = 0

    for line in source:
        parts = line.split()

        if len(parts) != 3:
            continue

        qrels_id, document_id, grade = parts

        original_query_id = query_mapping[qrels_id]

        # Grades 1-4 are relevant; grade 5 is non-relevant.
        relevance = 1 if int(grade) <= 4 else 0

        output.write(
            f"{original_query_id} 0 {document_id} {relevance}\n"
        )

        count += 1

print("Original queries:", len(query_ids))
print("Relevance judgements converted:", count)
print("Output: data/cranqrel_trec.txt")
