from pathlib import Path
from collections import defaultdict

QRELS_FILE = Path("data/cranqrel_binary.txt")
RESULTS_DIR = Path("results")

# Load relevance judgements.
# Only grade 1 is relevant in the binary file.
qrels = defaultdict(set)

with QRELS_FILE.open() as f:
    for line in f:
        parts = line.split()
        if len(parts) != 4:
            continue

        qid, iteration, doc_id, relevance = parts

        if int(relevance) > 0:
            qrels[qid].add(doc_id)

# Evaluate each result file.
for result_file in sorted(RESULTS_DIR.glob("*.txt")):

    results = defaultdict(list)

    with result_file.open() as f:
        for line in f:
            parts = line.split()
            if len(parts) != 6:
                continue

            qid, iteration, doc_id, rank, score, run_name = parts

            results[qid].append((int(rank), doc_id))

    average_precision_values = []
    recall_values = []
    precision_values = []

    for qid, relevant_docs in qrels.items():

        if not relevant_docs:
            continue

        ranked_results = sorted(
            results.get(qid, []),
            key=lambda item: item[0]
        )

        relevant_found = 0
        precision_sum = 0.0
        retrieved_relevant = set()

        for rank, doc_id in ranked_results:
            if doc_id in relevant_docs:
                relevant_found += 1
                precision_sum += relevant_found / rank
                retrieved_relevant.add(doc_id)

        average_precision = (
            precision_sum / len(relevant_docs)
        )

        recall = len(retrieved_relevant) / len(relevant_docs)

        top_10_relevant = sum(
            1 for rank, doc_id in ranked_results
            if rank <= 10 and doc_id in relevant_docs
        )

        precision_at_10 = top_10_relevant / 10

        average_precision_values.append(average_precision)
        recall_values.append(recall)
        precision_values.append(precision_at_10)

    query_count = len(average_precision_values)

    mean_ap = sum(average_precision_values) / query_count
    mean_recall = sum(recall_values) / query_count
    mean_p10 = sum(precision_values) / query_count

    print("\nConfiguration:", result_file.stem)
    print("Queries evaluated:", query_count)
    print(f"MAP: {mean_ap:.4f}")
    print(f"Mean Recall@1000: {mean_recall:.4f}")
    print(f"Mean P@10: {mean_p10:.4f}")
