import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ShutterflyLTV {

	private static String CUSTOMER_TYPE = "CUSTOMER";
	private static String SITE_VISIT_TYPE = "SITE_VISIT";
	private static String ORDER_TYPE = "ORDER";
	private static HashMap<String, String> customerIdNameMap = new HashMap<>();
	private static HashMap<String, HashMap<Integer, Integer>> customerIdWeekCountMap = new HashMap<>();
	private static HashMap<String, HashMap<String, Double>> customerIdOrderIdAmountMap = new HashMap<>();

	/* Iterates over the events data,prepares json object using json parsers and different map */
	static void ingestData(String[] events) {
		JSONParser parser = new JSONParser();
		for (String event : events) {
			try {
				Object obj = parser.parse(event);
				JSONObject jsonObject = (JSONObject) obj;
				String type = (String) jsonObject.get("type");
				String cusId = (String) jsonObject.get("customer_id");
				// String verb = (String) jsonObject.get("verb");
				if (type.equals(CUSTOMER_TYPE)) {
					String lastName = (String) jsonObject.get("last_name");
					if (lastName != null) {
						customerIdNameMap.put(cusId, lastName);
					} else {
						customerIdNameMap.put(cusId, cusId);
					}
				} else if (type.equals(SITE_VISIT_TYPE)) {
					String eventTime = (String) jsonObject.get("event_time");
					updateCustomerIdWeekCountMap(cusId, eventTime);
				} else if (type.equals(ORDER_TYPE)) {
					String orderId = (String) jsonObject.get("key");
					String amountUsd = (String) jsonObject.get("total_amount");
					Double amount = Double.parseDouble(amountUsd.replace("USD", "").trim());
					updateCustomerIdOrderIdAmountMap(cusId, orderId, amount);
				}

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	/* Gets the TopX LTV customers from input events */
	static List<String> topXSimpleLTVCustomers(int x) {
		HashMap<String, Double> custIdLtv = buildCustIdLTVMap();
		Map<String, Double> custIdLTV = sortByComparator(custIdLtv);
		int count = 1;
		List<String> topXLTVCustomersList = new ArrayList<String>();
		for (String key : custIdLTV.keySet()) {
			topXLTVCustomersList.add(customerIdNameMap.get(key));
			if (count == x) {
				return topXLTVCustomersList;
			}
			count++;
		}
		return topXLTVCustomersList;
	}

	/* Builds custIdLTV map where key is customerid and value is LTV */
	private static HashMap<String, Double> buildCustIdLTVMap() {
		HashMap<String, Double> custIdWeekAvgMap = new HashMap<>();
		for (Entry<String, HashMap<Integer, Integer>> entry : customerIdWeekCountMap.entrySet()) {
			String cusId = entry.getKey();
			HashMap<Integer, Integer> weekCountMap = entry.getValue();
			int weekAvg = 0;
			for (Entry<Integer, Integer> weekCountEntry : weekCountMap.entrySet()) {
				weekAvg += weekCountEntry.getValue();
			}
			custIdWeekAvgMap.put(cusId, (double) (weekAvg / 52));
		}

		HashMap<String, Double> custIdLTVMap = new HashMap<>();
		for (Entry<String, HashMap<String, Double>> entry : customerIdOrderIdAmountMap.entrySet()) {
			String cusId = entry.getKey();
			HashMap<String, Double> orderIdAmountMap = entry.getValue();
			double amtAvg = 0;
			for (Entry<String, Double> orderIdAmountEntry : orderIdAmountMap.entrySet()) {
				amtAvg += orderIdAmountEntry.getValue();
			}

			double a = (amtAvg / orderIdAmountMap.size()) * custIdWeekAvgMap.get(cusId);
			double ltv = 52 * a * 10;
			custIdLTVMap.put(cusId, ltv);
		}
		return custIdLTVMap;
	}

	/* Sorts the Map in descending order using maps values */
	private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap) {

		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	/* prepares updateCustomerIdOrderIdAmountMap map from customerid ,orderId amount */
	private static void updateCustomerIdOrderIdAmountMap(String cusId, String orderId, Double amount) {
		if (!customerIdOrderIdAmountMap.containsKey(cusId)) {
			customerIdOrderIdAmountMap.put(cusId, new HashMap<String, Double>());
		}

		HashMap<String, Double> orderIdAmountMap = customerIdOrderIdAmountMap.get(cusId);

		orderIdAmountMap.put(orderId, amount);
	}

	/*
	 * prepares customeridweekcount map from customerid and eventtime. calculates week of a year from eventtime
	 */
	private static void updateCustomerIdWeekCountMap(String cusId, String eventTime) {
		int week = getWeekNo(eventTime);
		if (!customerIdWeekCountMap.containsKey(cusId)) {
			customerIdWeekCountMap.put(cusId, new HashMap<Integer, Integer>());
		}

		HashMap<Integer, Integer> weekCountMap = customerIdWeekCountMap.get(cusId);
		if (!weekCountMap.containsKey(week)) {
			weekCountMap.put(week, 0);
		}

		int count = weekCountMap.get(week) + 1;
		weekCountMap.put(week, count);
	}

	/* Get weeks Number of a year given event date */
	static int getWeekNo(String eventDate) {
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss.SSS'Z'");
		Calendar calendar = null;
		try {
			Date date = sdf.parse(eventDate);
			calendar = new GregorianCalendar();
			calendar.setTime(date);
		} catch (java.text.ParseException e) {
			System.out.println(e.getMessage());
		}

		return calendar.get(Calendar.WEEK_OF_YEAR);
	}
}
