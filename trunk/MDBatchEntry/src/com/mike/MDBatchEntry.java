package com.mike;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class MDBatchEntry {
	
	public enum Frequency {
		DAILY(null), 
		WEEKLY(FrequencySpecifier.DAYSOFTHEWEEK), 
		BIMONTHLY(FrequencySpecifier.DAYSOFTHEWEEK), 
		MONTHLY(FrequencySpecifier.DAYSOFTHEMONTH), 
		QUARTERLY(FrequencySpecifier.DAYSOFTHEMONTH), 
		BIANUALLY(FrequencySpecifier.DAYSOFTHEMONTH), 
		ANUAL(FrequencySpecifier.DATE);
		
		private final FrequencySpecifier specifier;
		
		private Frequency(FrequencySpecifier specifier){
			this.specifier = specifier;
		}
		
		public static String getHelpString(){
			String help = "What is the entry frequency, can be any of the following:\n";
			for(Frequency f : values()){
				help += "\t"+f.toString();
				if(f.specifier != null){
					help += " - requires specifier\n\t\t"+f.specifier.getHelpString();
				}
				help += "\n";
			}
			return help;
		}
		
		public FrequencySpecifier getSpecifier(){
			return specifier;
		}
		
	}
	
	public enum Day {
		MON(Calendar.MONDAY), 
		TUE(Calendar.TUESDAY), 
		WED(Calendar.WEDNESDAY), 
		THR(Calendar.THURSDAY), 
		FRI(Calendar.FRIDAY), 
		SAT(Calendar.SATURDAY), 
		SUN(Calendar.SUNDAY);
		
		private final int dayField;
		private Day(int dayfield){
			this.dayField = dayfield;
		}
		
		public int getDayField(){
			return dayField;
		}
	}
	
	public enum FrequencySpecifier {
		DAYSOFTHEWEEK("Must be three letter day values in a comma seperated list: MON,TUE,WED,THR,FRI,SAT,SUN"), 
		DAYSOFTHEMONTH("Must be a comma seperated list, i.e. 3,5,18..."), 
		DATE("Must be a formatted date of some kind.  12/12/12");
		
		private final String helpTxt;
		
		private FrequencySpecifier(String helpTxt){
			this.helpTxt = helpTxt;
		}
		
		public static String getHelpStrings(){
			StringBuilder builder = new StringBuilder();
			for(FrequencySpecifier val : values()){
				builder.append(val.getHelpString());
				builder.append("\n");
			}
			return builder.toString();
		}
		
		public String getHelpString(){
			return helpTxt;
		}
	}

    public static void main( String[] args ) {
    	AutoHelpParser parser = new AutoHelpParser();
        CmdLineParser.Option<String> descriptionOpt = parser.addHelp(
                parser.addStringOption('d', "description"),
                "Who is this entry going to/from, i.e. Pizza Hut or Best Buy");
        CmdLineParser.Option<String> categoryOpt = parser.addHelp(
                parser.addStringOption('c', "category"),
                "What category is this in, i.e. Dining or Groceries");
        CmdLineParser.Option<Double> ammountOpt = parser.addHelp(
                parser.addDoubleOption('a', "ammount"),
                "How much will these entries be for");
        CmdLineParser.Option<Boolean> withdrawlOpt = parser.addHelp(
                parser.addBooleanOption('w', "withdrawl"),
                "Are these entries concidered withdrawls or deposits?");
        CmdLineParser.Option<Date> startOpt = parser.addHelp(
        		parser.addDateOption('s', "start"),
        		"The Start Date for the entries");
        CmdLineParser.Option<Date> endOpt = parser.addHelp(
        		parser.addDateOption('e', "end"),
        		"The End Date for the entries");
        CmdLineParser.Option<Frequency> frequencyOpt = parser.addHelp(
        		parser.addEnumOption('f', "frequency", Frequency.class),
        		Frequency.getHelpString());
        CmdLineParser.Option<String> frequencySpecOpt = parser.addHelp(
        		parser.addStringOption('r', "specifier"),
        		"See help for \"frequency\" (f) option.");
        CmdLineParser.Option<Boolean> help = parser.addHelp(
                parser.addBooleanOption('h', "help"),
                "Show this help message");

        try {
            parser.parse(args);
        } catch ( CmdLineParser.OptionException e ) {
            System.err.println(e.getMessage());
            parser.printUsage();
            System.exit(2);
        }

        if ( parser.getOptionValue(help) != null && parser.getOptionValue(help) == true ) {
            parser.printUsage();
            System.exit(0);
        }

        String description = parser.getOptionValue(descriptionOpt);
        if(description == null){
        	System.err.println("description is required");
        	parser.printUsage();
        	System.exit(0);
        }
        String category = parser.getOptionValue(categoryOpt);
        if(category == null){
        	System.err.println("category is required");
        	parser.printUsage();
        	System.exit(0);
        }
        Double amount = parser.getOptionValue(ammountOpt);
        if(amount == null){
        	System.err.println("amount is required");
        	parser.printUsage();
        	System.exit(0);
        }
        if(amount < 0)
        	amount = -amount;
        Boolean isWithdrawl = parser.getOptionValue(withdrawlOpt, false);
        Date start = parser.getOptionValue(startOpt, new Date());
        Date end = parser.getOptionValue(endOpt);
        if(end == null){
        	System.err.println("an end date must be specified.");
        	parser.printUsage();
        	System.exit(0);
        }
        Frequency frequency = parser.getOptionValue(frequencyOpt);
        if(frequency == null){
        	System.err.println("frequency is required");
        	parser.printUsage();
        	System.exit(0);
        }
        
        String frequencySpec = parser.getOptionValue(frequencySpecOpt);
        if(frequency.getSpecifier() != null && frequencySpec == null){
        	System.err.println("frequency specification of type "+frequency.getSpecifier().toString()+" is required");
        	System.err.println(frequency.getSpecifier().getHelpString());
        	parser.printUsage();
        	System.exit(0);	
        }
        
        ArrayList<Date> occurences = new ArrayList<Date>();
        if(frequency.getSpecifier() != null){
        	try {
	        	switch(frequency.getSpecifier()){
				case DATE:
					occurences.add(DateFormat.getDateInstance().parse(frequencySpec));
					break;
				case DAYSOFTHEMONTH:
					Calendar monthsCalendar = Calendar.getInstance();
					monthsCalendar.setTime(start);
					ArrayList<Integer> daysOfTheMonth = new ArrayList<Integer>();
					String[] dayStrs = frequencySpec.split(",");
					for(String dayStr : dayStrs){
						Integer day = Integer.parseInt(dayStr);
						if(day > 31)
							throw new InvalidParameterException("Day values cannot be greater than 31");
						if(day < 1)
							throw new InvalidParameterException("Day values cannot be less than 1");
						if(!daysOfTheMonth.contains(day.intValue()))
							daysOfTheMonth.add(day);
					}
					Collections.sort(daysOfTheMonth);
					int monthInc = 0;
					switch(frequency){
					case QUARTERLY:
						monthInc = 3;
						break;
					case BIANUALLY:
						monthInc = 6;
						break;
					default:
					case MONTHLY:
						monthInc = 1;
						break;
					}
					while(monthsCalendar.getTime().before(end)){
						for(Integer day : daysOfTheMonth){
							monthsCalendar.set(Calendar.DAY_OF_MONTH, day);
							occurences.add(monthsCalendar.getTime());
						}
						monthsCalendar.add(Calendar.MONTH, monthInc);
					}
					break;
				default:
				case DAYSOFTHEWEEK:
					Calendar weeksCalendar = Calendar.getInstance();
					weeksCalendar.setTime(start);
					weeksCalendar.setFirstDayOfWeek(Calendar.SUNDAY);
					String[] daysStr = frequencySpec.split(",");
					ArrayList<Integer> days = new ArrayList<Integer>();
					for(String dayStr : daysStr){
						Day day = Day.valueOf(dayStr.trim().toUpperCase());
						if(!days.contains(day.getDayField()))
							days.add(day.getDayField());
					}
					int dayInc = 0;
					switch(frequency){
					case BIMONTHLY:
						dayInc = 14;
						break;
					default:
					case WEEKLY:
						dayInc = 7;
						break;
					}
					weeksCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
					while(weeksCalendar.getTime().before(end)){
						Date startDate = weeksCalendar.getTime();
						for(Integer dayField : days){
							weeksCalendar.set(Calendar.DAY_OF_WEEK, dayField);
							if(!weeksCalendar.getTime().before(start) && !weeksCalendar.getTime().after(end))
								occurences.add(weeksCalendar.getTime());
						}
						weeksCalendar.setTime(startDate);
						weeksCalendar.add(Calendar.DATE, dayInc);
					}
					break;
	        	}
        	} catch (Exception e) {
        		System.out.println(e.getMessage());
        		System.exit(1);
        	}
        }
        
        File outputFile = new File(description.replace(" ", "_")+".txt");
        if(!outputFile.exists()){
			try {
				outputFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(3);
			}
        }
        
        BufferedWriter writer = null;
        try{
        	writer = new BufferedWriter(new FileWriter(outputFile));
        	DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
        	String outputFormat = "%s,%s,%s,%s,%.2f%n";
        	for(Date date : occurences){
            	writer.write(String.format(outputFormat, 
            			format.format(date), (isWithdrawl ? "Debit" : "Dep"), 
            			description, category, (isWithdrawl ? -amount : amount)));
            }
        	writer.flush();
        } catch (Exception e){
        	System.out.println(e.getMessage());
        	System.exit(4);
        } finally {
        	if(writer != null)
        		try { writer.close(); } catch (Exception e){}
        }
    }
}
