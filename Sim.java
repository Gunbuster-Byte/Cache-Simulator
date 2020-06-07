import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Stack;

//Program by Jason Rodriguez
public class Sim
{
	public static void main(String args[]) throws FileNotFoundException
	{
		int cache_size = Integer.parseInt(args[0]);
		int assoc = Integer.parseInt(args[1]);
		int REPL = Integer.parseInt(args[2]);
		int WB = Integer.parseInt(args[3]);
		File Trace = new File(args[4]);
		Scanner Trace_file = new Scanner(Trace);

		int block_size = 64;
		int sets = cache_size/(assoc*block_size);
		String cache_tags[][] = new String[sets][assoc];
		String[][] dirty = new String[sets][assoc];

		for(int i = 0; i < sets; i++)
		{
			for(int j = 0; j < assoc; j++)
			{
				cache_tags[i][j] = "";
				dirty[i][j] = "";
			}
		}

		long[][] replc_metadata = new long[sets][assoc];
		char op;
		long address;
		long block_num;
		long block_address;
		int Hits = 0;
		int WRITE = 0;
		int READ = 0;
		boolean access_hit;
		int index_hit;
		String replace = "";
		int accesses = 0;
		String index = "";
		String tag = "";
		int ind = sets;
		int base2 = 0;

		while(ind > 1)
		{
			ind = ind / 2;
			base2++;
		}

		while(Trace_file.hasNextLine())
		{
			//Parsing
			String line = Trace_file.nextLine();
			String[] tokens = line.split(" ");
			op = tokens[0].charAt(0);
			String[] Hex = tokens[1].split("0x");
			String convert1 = new BigInteger(Hex[1], 16).toString(2);
			BigInteger convert = new BigInteger(Hex[1], 16);
			address = Long.parseUnsignedLong(convert.toString());

			while(convert1.length() != 64)
				convert1 = "0" + convert1;

			index = convert1.substring((64 - base2 - 6), (64 - 6));
			tag = convert1.substring(0, (64 - base2 - 6));
			int index2 = Integer.parseInt(index, 2);
			access_hit = false;
			index_hit = -1;
			accesses++;

			for(int j = 0; j < assoc; j++)
			{
				if(tag.contentEquals((cache_tags[index2][j])))
				{
					Hits++;
					access_hit = true;
					index_hit = j;
					break;
				}
			}

			//FIFO
			if(REPL == 1)
			{
				if(access_hit)
				{
					if(op == 'W' && WB == 1)
						dirty[index2][index_hit] = '1' + tag;
					if(WB == 0 && op == 'W')
						WRITE++;
				}
				else
				{
					int j = 0;

					for(j = 0; j < assoc; j++)
					{
						if(replc_metadata[index2][j] == 0)
						{
							replc_metadata[index2][j] = accesses;
							cache_tags[index2][j] = tag;
							if(op == 'R')
							READ++;
							if(WB == 0 && op == 'W')
							WRITE++;
							if(WB == 1 && op == 'W')
							{
							dirty[index2][j] = '1' + tag;
							}
							break;
						}
					}

					if(op == 'W')
						READ++;
					if(j == assoc)
					{
					if(WB == 1 && dirty[index2][0].contentEquals('1' + (cache_tags[index2][0])))
						WRITE++;
					for(int i = 0; i < assoc - 1; i++)
					{
						replc_metadata[index2][i] = replc_metadata[index2][i + 1];
						dirty[index2][i] = dirty[index2][i + 1];
						cache_tags[index2][i] = cache_tags[index2][i + 1];
					}
					eplc_metadata[index2][assoc - 1] = accesses;

					cache_tags[index2][assoc - 1] = tag;
					if(op == 'W' && WB == 1)
						dirty[index2][assoc - 1] = '1' + tag;
					if(op == 'R')
						READ++;
					if(op == 'R' && WB == 1)
						dirty[index2][assoc - 1] = "";

					if(WB == 0 && op == 'W')
						WRITE++;
					}
				}
			}
			//LRU
			if(REPL == 0)
			{
				if(access_hit)
				{
					//LRU update
					replc_metadata[index2][index_hit] = accesses;
					if(op == 'W' && WB == 1)
						dirty[index2][index_hit] = '1' + tag;
					if(WB == 0 && op == 'W')
						WRITE++;
				}
				else
				{
					int j = 0;

					for(j = 0; j < assoc; j++)
					{
						if(replc_metadata[index2][j] == 0)
						{
							replc_metadata[index2][j] = accesses;
							cache_tags[index2][j] = tag;
							if(op == 'R')
								READ++;
							if(WB == 0 && op == 'W')
								WRITE++;
							if(WB == 1 && op == 'W')
								dirty[index2][j] = '1' + tag;
							break;
						}
					}
					if(op == 'W')
						READ++;
					if(j == assoc)
					{
						long temp[] = Arrays.copyOf(replc_metadata[index2], replc_metadata[index2].length);
						Arrays.sort(temp);

						for(int i = 0; i < assoc; i++)
						{
							if(replc_metadata[index2][i] == temp[0])
							{
							replc_metadata[index2][i] = accesses;
							if(WB == 1 && dirty[index2][i].contentEquals('1' + (cache_tags[index2][i]))) {
								WRITE++;
							if(op == 'W')
								dirty[index2][i] = '1' + tag;
							}
							if(op == 'R')
								READ++;
							if(op == 'R' && WB == 1)
								dirty[index2][i] = "";
							if(WB == 0 && op == 'W')
								WRITE++;
							cache_tags[index2][i] = tag;
							break;
							}
						}

					}
				}
			}
		}
		double miss_rat = 1 - (double)Hits/accesses;
		System.out.println("Miss Ratio: " + miss_rat);
		System.out.println("Write Memory Accesses: " + WRITE);
		System.out.println("Read Memory Accesses: "+ READ);
		}
}
