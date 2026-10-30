input_file = "data/cranqrel"
output_file = "data/cranqrel_binary.txt"

count_relevant = 0
count_non_relevant = 0

with open(input_file, "r") as source, \
     open(output_file, "w") as output:

    for line in source:
        parts = line.split()

        if len(parts) < 3:
            continue

        query_id = int(parts[0])
        document_id = int(parts[1])
        original_grade = int(parts[2])

        if original_grade in (1, 2, 3):
            binary_grade = 1
            count_relevant += 1
        else:
            binary_grade = 0
            count_non_relevant += 1

        output.write(
            f"{query_id} 0 {document_id} {binary_grade}\n"
        )

print("Binary qrels created successfully.")
print("Relevant judgements:", count_relevant)
print("Non-relevant judgements:", count_non_relevant)
print("Output:", output_file)
