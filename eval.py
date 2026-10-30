from pathlib import Path
from collections import defaultdict

# Run from the cranfield-search project directory:
#   python3 evaluate_corrected.py
#
# IMPORTANT:
# cran.qry uses original, non-consecutive IDs (001, 002, 004, 008, ...).
# cranqrel numbers queries sequentially by position (1, 2, 3, 4, ...).
# This script maps qrels query number N to the Nth query ID in cran.qry.

QUERY_FILE = Path("data/cran.qry")
QRELS_FILE = Path("data/cranqrel")
RESULTS_DIR = Path("results")

def read_query_ids(path):
    ids = []
    with path.open("r", encoding="utf-8") as file:
        for line in file:
            parts = line.split()
            if len(parts) >= 2 and parts[0] == ".I":
                ids.append(str(int(parts[1])))
    return ids

query_ids_in_order = read_query_ids(QUERY_FILE)

if len(query_ids_in_order) != 225:
    raise ValueError(
        f"Expected 225 queries in {QUERY_FILE}, found {len(query_ids_in_order)}."
    )

# Map sequential qrels query numbers (1..225) to original query IDs.
qrels_to_original_id = {
    str(position): original_id
    for position, original_id in enumerate(query_ids_in_order, start=1)
}

qrels = defaultdict(set)
qrels_query_numbers = set()

with QRELS_FILE.open("r", encoding="utf-8") as file:
    for line in file:
        parts = line.split()
        if len(parts) < 3:
            continue

        qrel_query_number, document_id, grade = parts[:3]

        if qrel_query_number not in qrels_to_original_id:
            raise ValueError(
                f"Unexpected qrels query number {qrel_query_number}. "
                "Expected sequential query numbers 1 through 225."
            )

        original_query_id = qrels_to_original_id[qrel_query_number]
        qrels_query_numbers.add(qrel_query_number)

        # Cranfield grades 1-4 are all positive relevance grades.
        # Grade 1 is strongest; grade 4 is weakest.
        if int(grade) > 0:
            qrels[original_query_id].add(document_id)

expected_qrels_numbers = {str(i) for i in range(1, 226)}
if qrels_query_numbers != expected_qrels_numbers:
    missing = sorted(
        expected_qrels_numbers - qrels_query_numbers, key=int
    )
    extra = sorted(
        qrels_query_numbers - expected_qrels_numbers, key=int
    )
    raise ValueError(
        f"Qrels query-number validation failed. Missing: {missing}; extra: {extra}"
    )

result_files = sorted(
    path for path in RESULTS_DIR.glob("*.txt")
    if path.name not in {"evaluation_summary.txt"}
)

if not result_files:
    raise FileNotFoundError(f"No result files found in {RESULTS_DIR}.")

for result_file in result_files:
    results = defaultdict(list)

    with result_file.open("r", encoding="utf-8") as file:
        for line in file:
            parts = line.split()
            if len(parts) < 6:
                continue

            query_id, _q0, document_id, rank, _score, _run_name = parts[:6]
            results[str(int(query_id))].append((int(rank), document_id))

    aps = []
    recalls = []
    p5_values = []
    p10_values = []

    for query_id in query_ids_in_order:
        relevant_docs = qrels[query_id]
        if not relevant_docs:
            raise ValueError(
                f"Query {query_id} has no positive relevance judgements."
            )

        ranked = sorted(results.get(query_id, []), key=lambda item: item[0])

        relevant_seen = set()
        relevant_count = 0
        precision_sum = 0.0

        for rank, document_id in ranked:
            if document_id in relevant_docs and document_id not in relevant_seen:
                relevant_seen.add(document_id)
                relevant_count += 1
                precision_sum += relevant_count / rank

        ap = precision_sum / len(relevant_docs)
        recall = len(relevant_seen) / len(relevant_docs)

        hits_at_5 = sum(
            1 for rank, doc in ranked
            if rank <= 5 and doc in relevant_docs
        )
        hits_at_10 = sum(
            1 for rank, doc in ranked
            if rank <= 10 and doc in relevant_docs
        )

        aps.append(ap)
        recalls.append(recall)
        p5_values.append(hits_at_5 / 5)
        p10_values.append(hits_at_10 / 10)

    count = len(query_ids_in_order)
    print(f"Configuration: {result_file.stem}")
    print(f"Queries evaluated: {count}")
    print(f"MAP: {sum(aps) / count:.4f}")
    print(f"Mean Recall@1000: {sum(recalls) / count:.4f}")
    print(f"Mean P@5: {sum(p5_values) / count:.4f}")
    print(f"Mean P@10: {sum(p10_values) / count:.4f}")
    print()

