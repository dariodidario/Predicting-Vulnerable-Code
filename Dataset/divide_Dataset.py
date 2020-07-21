csvfile = open('dataset.csv', 'r').readlines()
filename = 1
for i in range(len(csvfile)):
	if i % 100 == 0:
		open(str(filename) + '.csv', 'w+').writelines(csvfile[i:i+100])
		filename += 1