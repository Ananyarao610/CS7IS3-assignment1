from pathlib import Path
from collections import defaultdict

# Run this script from the cranfield-search project directory:
#   python3 evaluate_p5.py
#
# It evaluates every results/*.txt run file against the binary Cranfield qrels.
# Relevance mapping used in cranqrel_binary.txt:
#   original grades 1, 2, 3 -> relevant (1)
#   original grade 4         -> non-relevant (0)

QRELS_FILE = Path("data/cranqrel_binary.txt")
RESULTS_DIR = Path("results")
CUTOFF = 1000

if not QRELS_FILE.exists():
    raise FileNotFoundError(f"Could not find {QRELS_FILE}. Run from the project directory.")
if not RESULTS_DIR.exists():
    raise FileNotFoundError(f"Could not find {RESULTS_DIR}.")

# Load relevant document IDs for each query.
qrels = defaultdict(set)

with QRELS_FILE.open("r", encoding="utf-8") as file:
    for line in file:
        parts = line.split()
        if len(parts) < 4:
            continue

        query_id, _iteration, document_id, relevance = parts[:4]

        if int(relevance) > 0:
            qrels[query_id].add(document_id)

# Only evaluate queries that have at least one relevant document.
evaluated_query_ids = sorted(
    (query_id for query_id, docs in qrels.items() if docs),
    key=lambda value: int(value)
)

if not evaluated_query_ids:
    raise ValueError("No relevant qrels found in the binary qrels file.")

result_files = sorted(
    file for file in RESULTS_DIR.glob("*.txt")
    if file.name != "evaluation_summary.txt"
)

if not result_files:
    raise FileNotFoundError(f"No result run files found in {RESULTS_DIR}.")

for result_file in result_files:
    # Store (rank, document ID) for each query.
    results = defaultdict(list)

    with result_file.open("r", encoding="utf-8") as file:
        for line in file:
            parts = line.split()
            if len(parts) < 6:
                continue

            query_id, _q0, document_id, rank, _score, _run_name = parts[:6]
            results[query_id].append((int(rank), document_id))

    average_precisions = []
    recalls = []
    precisions_at_5 = []
    precisions_at_10 = []

    for query_id in evaluated_query_ids:
        relevant_docs = qrels[query_id]
        ranked_results = sorted(results.get(query_id, []), key=lambda item: item[0])

        relevant_found = 0
        precision_sum = 0.0
        retrieved_relevant = set()

        for rank, document_id in ranked_results:
            if document_id in relevant_docs and document_id not in retrieved_relevant:
                relevant_found += 1
                precision_sum += relevant_found / rank
                retrieved_relevant.add(document_id)

        # Average Precision: relevant-hit precision values divided by
        # the total number of relevant documents for this query.
        average_precision = precision_sum / len(relevant_docs)
        recall = len(retrieved_relevant) / len(relevant_docs)

        relevant_in_top_5 = sum(
            1 for rank, document_id in ranked_results
            if rank <= 5 and document_id in relevant_docs
        )
        relevant_in_top_10 = sum(
            1 for rank, document_id in ranked_results
            if rank <= 10 and document_id in relevant_docs
        )

        # Precision at k uses k as the denominator, even if fewer than k
        # documents were returned for a query.
        p5 = relevant_in_top_5 / 5
        p10 = relevant_in_top_10 / 10

        average_precisions.append(average_precision)
        recalls.append(recall)
        precisions_at_5.append(p5)
        precisions_at_10.append(p10)

    query_count = len(evaluated_query_ids)

    print(f"Configuration: {result_file.stem}")
    print(f"Queries evaluated: {query_count}")
    print(f"MAP: {sum(average_precisions) / query_count:.4f}")
    print(f"Mean Recall@{CUTOFF}: {sum(recalls) / query_count:.4f}")
    print(f"Mean P@5: {sum(precisions_at_5) / query_count:.4f}")
    print(f"Mean P@10: {sum(precisions_at_10) / query_count:.4f}")
    print()

