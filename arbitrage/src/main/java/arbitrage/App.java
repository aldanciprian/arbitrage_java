package arbitrage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.sql.*;
import java.util.Date;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.bitfinex.v2.BitfinexExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.exceptions.ExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service.State;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

public class App {

	public enum STATE {
		INIT, SEARCH_POTENTIAL, REQ_EXECUTE_TRANSACTION, PENDING_EXECUTED_TRANSACTION, EXECUTED_TRANSACTION, REQUEST_SWAP, PENDING_SWAP, EXECUTED_SWAP
	}

	public static List<String> exchangesNames = null;
	public static Map<String, List<CurrencyPair>> tradable_pairs = null;
	public static Map<String, Exchange> exchanges = null;
	public static Map<String, List<CurrencyPair>> pairPerExchange = null;
	public static Map<String, Map<CurrencyPair, Ticker>> all_tickers = null;
	public static List<PotentialPair> ppair_list = null;
	public static Map<String, List<CurrencyPair>> all_counter_symbols = null;
	public static Map<String, List<CurrencyPair>> all_filters = null;
	public static Map<String, Map<Currency, CurrencyMetaData>> all_currency_metadata = null;
	public static Map<String, Map<CurrencyPair, CurrencyPairMetaData>> all_currencypair_metadata = null;

	public static Connection conn = null;
	public static String positive_pairs = "positive_pairs";
	public static String state_table = "state";
	public static String substate_transact = "substate_transaction";
	public static String substate_swap = "substate_swap";

	public static STATE state = STATE.INIT;
	public static PotentialPair transact_ppair = null;

	public static long get_exchange_meta_timer_max = 5; // the number of loops to get exchange meta
	public static long get_exchange_meta_timer = get_exchange_meta_timer_max; // the number of loops to get exchange
																				// meta

	public static long last_trade_delay = 180000; // 3 minutes
	public static double potential_delta_profit_procent = 0.15;
	public static double dollar_ammount = 0.004; // in BTC aprox 50 $
	public static Currency counter = Currency.BTC;
	public static double contingent_procent = 0.01; // how much procent should be add to the price to be bought or sold
	public static long loop_delay = 5000; // miliseconds of loop
	public static boolean apply_filter = false;

	public static void insertState( STATE _state )
	{
		Statement stmt;
		Timestamp tstmp = new Timestamp(System.currentTimeMillis());
		String query = "";
		String cols ="";
		
		try {
			stmt = conn.createStatement();
			cols += "'"+tstmp.toString()+"'";
			cols += ",";
			cols += "'"+_state.toString()+"'";
			query = "insert into " + state_table + "(tstmp,state) values ("+cols+")";
			System.out.println(query);
			
			stmt.execute(query);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
	public static void changeState(STATE _state)
	{
		state = _state;
		insertState(_state);
	}
	public static STATE getStateMachine() {
		int id;
		String temp_state = "";
		Timestamp tstmp;

		try {
			Statement stmt = conn.createStatement();
			String query = "select * from " + state_table + " order by id desc limit 1";
			System.out.println(query);
			// execute the query, and get a java resultset
			ResultSet rs = stmt.executeQuery(query);
			// iterate through the java resultset
			while (rs.next()) {
				id = rs.getInt("id");
				temp_state = rs.getString("state");
				tstmp = rs.getTimestamp("tstmp");

				// print the results
				System.out.format("%s, %s, %s \n", tstmp.toString(),temp_state,id);
			}
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		if (temp_state == "") {
			return STATE.INIT;
		} else {
			switch (temp_state) {
			case "INIT":
				return STATE.INIT;
			case "SEARCH_POTENTIAL":
				return STATE.SEARCH_POTENTIAL;
			case "REQ_EXECUTE_TRANSACTION":
				return STATE.REQ_EXECUTE_TRANSACTION;
			case "PENDING_EXECUTED_TRANSACTION":
				return STATE.PENDING_EXECUTED_TRANSACTION;
			case "EXECUTED_TRANSACTION":
				return STATE.EXECUTED_TRANSACTION;
			case "REQUEST_SWAP":
				return STATE.REQUEST_SWAP;
			case "PENDING_SWAP":
				return STATE.PENDING_SWAP;
			case "EXECUTED_SWAP":
				return STATE.EXECUTED_SWAP;
			}
		}
		Thread.dumpStack();
		System.exit(-1);

		return STATE.INIT;
	}

	public static void connectDB() {
		try {
			// Step 1: Allocate a database 'Connection' object
			String query = "";
			String cols = "";
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/arbitrage", "ciprian", "ciprian");
			// MySQL: "jdbc:mysql://hostname:port/databaseName", "username", "password"
			// Step 2: Allocate a 'Statement' object in the Connection
			Statement stmt = conn.createStatement();

			cols += " tstmp TIMESTAMP ";
			cols += ",";
			cols += " pair VARCHAR(40) ";
			cols += ",";
			cols += " buy_exchange VARCHAR(40) ";
			cols += ",";
			cols += " buy_price DOUBLE ";
			cols += ",";
			cols += " sell_exchange VARCHAR(40) ";
			cols += ",";
			cols += " sell_price DOUBLE ";
			cols += ",";
			cols += " delta_procent DOUBLE ";
			cols += ",";
			cols += " last_buy_tstmp TIMESTAMP ";
			cols += ",";
			cols += " last_sell_tstmp TIMESTAMP ";

			query += "create table if not exists " + positive_pairs + " ( " + cols + " )";

			// System.out.println(query);

			stmt.execute(query);

			// state table
			query = "";
			cols = "";

			cols += " tstmp TIMESTAMP ";
			cols += ",";
			cols += " state VARCHAR(40) ";
			cols += ",";
			cols += " id INT UNSIGNED NOT NULL AUTO_INCREMENT ";
			cols += ",";
			cols += " PRIMARY KEY (id) ";

			query += "create table if not exists " + state_table + " ( " + cols + " )";

			System.out.println(query);

			stmt.execute(query);

			query = "";
			cols = "";

			cols += " tstmp TIMESTAMP ";
			cols += ",";
			cols += " id INT UNSIGNED NOT NULL ";
			cols += ",";
			cols += " buy_state VARCHAR(40) ";
			cols += ",";
			cols += " buy_orderid VARCHAR(40) ";
			cols += ",";
			cols += " buy_exchange VARCHAR(40) ";
			cols += ",";
			cols += " sell_state VARCHAR(40) ";
			cols += ",";
			cols += " sell_orderid VARCHAR(40) ";
			cols += ",";
			cols += " sell_exchange VARCHAR(40) ";
			cols += ",";

			cols += " FOREIGN KEY (id) REFERENCES " + state_table + "(id)";

			query += "create table if not exists " + substate_transact + " ( " + cols + " )";

			System.out.println(query);

			stmt.execute(query);

		} catch (SQLException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	public static void connectExchanges() {
		for (String s : exchangesNames) {
			ExchangeSpecification spec = null;
			switch (s) {
			case "bitfinex":
				spec = new BitfinexExchange().getDefaultExchangeSpecification();
				break;
			case "binance":
				spec = new BinanceExchange().getDefaultExchangeSpecification();
				break;
			case "poloniex":
				spec = new PoloniexExchangeV2().getDefaultExchangeSpecification();
				break;
			}

			spec.setApiKey(System.getenv(s + "_APIKEY"));
			spec.setSecretKey(System.getenv(s + "_SECRET"));
			exchanges.put(s, ExchangeFactory.INSTANCE.createExchange(spec));
		}
	}

	public static void init() {

		tradable_pairs = new HashMap<String, List<CurrencyPair>>();

		exchangesNames = new Vector<String>();
		exchangesNames.add("bitfinex");
		exchangesNames.add("binance");
		exchangesNames.add("poloniex");

		ppair_list = new Vector<PotentialPair>();

		exchanges = new HashMap<String, Exchange>();

		all_tickers = new HashMap<String, Map<CurrencyPair, Ticker>>();

		all_counter_symbols = new HashMap<String, List<CurrencyPair>>();

		all_filters = new HashMap<String, List<CurrencyPair>>();

		all_currency_metadata = new HashMap<String, Map<Currency, CurrencyMetaData>>();
		all_currencypair_metadata = new HashMap<String, Map<CurrencyPair, CurrencyPairMetaData>>();

		// Open the file
		try {
			FileInputStream fstream = new FileInputStream("src/main/resources/whitepairs.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;

			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				// System.out.println (strLine);
				if (strLine.contains("#")) {
					// jump over comment lines
					continue;
				}
				if (strLine.contains("=")) {
					String[] parts = strLine.split("=");
					String[] base_counter = parts[1].split("/");
					if (all_filters.containsKey(parts[0])) {
						all_filters.get(parts[0]).add(new CurrencyPair(base_counter[0], base_counter[1]));
					} else {
						List<CurrencyPair> pairs = new Vector<CurrencyPair>();
						pairs.add(new CurrencyPair(base_counter[0], base_counter[1]));
						all_filters.put(parts[0], pairs);
					}
				}
			}

			// Set<String> keys = all_filters.keySet();
			// for ( String key : keys)
			// {
			// System.out.print(key+" ");
			// for ( CurrencyPair cp : all_filters.get(key)) {
			// System.out.print(cp.toString()+",");
			// }
			// System.out.println();
			// }

			// Close the input stream
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.exit(0);

	}

	public static void setSymbols() {
		for (String key : exchangesNames) {
			List<CurrencyPair> eth_symbols = new Vector<CurrencyPair>();
			List<CurrencyPair> symbols = exchanges.get(key).getExchangeSymbols();

			for (CurrencyPair cp : symbols) {
				// check if it in white list
				boolean isWhite = false;
				for (CurrencyPair white_cp : all_filters.get(key)) {
					if (cp.compareTo(white_cp) == 0) {
						isWhite = true;
						break;
					}
				}

				if (apply_filter == true) {
					if (isWhite == true) {
						if ((cp.counter == counter) /* || (cp.base == Currency.ETH) */ ) {

							eth_symbols.add(cp);
						}
					}
				} else {
					if ((cp.counter == counter) /* || (cp.base == Currency.ETH) */ ) {

						eth_symbols.add(cp);
					}
				}
			}
			all_counter_symbols.put(key, eth_symbols);
		}
		for (String key : exchangesNames) {
			for (String key2 : exchangesNames) {
				if (key != key2) {
					List<CurrencyPair> common_symbols = new Vector<CurrencyPair>();
					for (CurrencyPair elem : all_counter_symbols.get(key)) {
						for (CurrencyPair elem2 : all_counter_symbols.get(key2)) {
							if (elem.compareTo(elem2) == 0) {
								common_symbols.add(elem);
								break;
							}
						}
					}
					if (!tradable_pairs.containsKey(key + ":" + key2)
							&& !tradable_pairs.containsKey(key2 + ":" + key)) {
						tradable_pairs.put(key + ":" + key2, common_symbols);
					}
				}
			}
		}

		pairPerExchange = new HashMap<String, List<CurrencyPair>>();
		for (String key : tradable_pairs.keySet()) {
			String[] exchanges_keys = key.split(":");
			for (CurrencyPair cp : tradable_pairs.get(key)) {
				if (pairPerExchange.containsKey(exchanges_keys[0])) {
					List<CurrencyPair> temp_list_cp = new Vector<CurrencyPair>();
					for (CurrencyPair cp2 : pairPerExchange.get(exchanges_keys[0])) {
						temp_list_cp.add(cp2);
					}

					boolean contains = false;
					for (CurrencyPair cp2 : temp_list_cp) {
						if (cp.compareTo(cp2) == 0) {
							contains = true;
							break;
						}
					}
					if (contains == false) {
						pairPerExchange.get(exchanges_keys[0]).add(cp);
					}
				} else {
					// System.out.println(exchanges_keys[0]+" adding to list");
					List<CurrencyPair> list_cp = new Vector<CurrencyPair>();
					list_cp.add(cp);
					pairPerExchange.put(exchanges_keys[0], list_cp);
				}
			}

			for (CurrencyPair cp : tradable_pairs.get(key)) {
				if (pairPerExchange.containsKey(exchanges_keys[1])) {
					List<CurrencyPair> temp_list_cp = new Vector<CurrencyPair>();
					for (CurrencyPair cp2 : pairPerExchange.get(exchanges_keys[1])) {
						temp_list_cp.add(cp2);
					}

					boolean contains = false;
					for (CurrencyPair cp2 : temp_list_cp) {
						if (cp.compareTo(cp2) == 1) {
							contains = true;
							break;
						}
					}
					if (contains == false) {
						pairPerExchange.get(exchanges_keys[1]).add(cp);
					}
				} else {
					List<CurrencyPair> list_cp = new Vector<CurrencyPair>();
					list_cp.add(cp);
					pairPerExchange.put(exchanges_keys[1], list_cp);
				}
			}
		}

	}

	public static void getTickers() {
		all_tickers.clear();
		List<Thread> workers = new Vector<Thread>();
		for (String key : exchangesNames) {
			MyRunnable r = new MyRunnable(key);
			Thread th = new Thread(r);
			workers.add(th);

			th.start();
		}

		for (Thread th : workers) {
			try {
				th.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void generatePotentialPairs() {
		ppair_list.clear();

		if (get_exchange_meta_timer == get_exchange_meta_timer_max) {
			// get meta
			all_currencypair_metadata.clear();
			all_currency_metadata.clear();

			for (String key : exchangesNames) {
				Map<Currency, CurrencyMetaData> exchange_currency_meta = null;
				Map<CurrencyPair, CurrencyPairMetaData> exchange_currencyPair_meta = null;

				ExchangeMetaData key_meta = exchanges.get(key).getExchangeMetaData();

				exchange_currency_meta = key_meta.getCurrencies();
				exchange_currencyPair_meta = key_meta.getCurrencyPairs();
				all_currency_metadata.put(key, exchange_currency_meta);
				all_currencypair_metadata.put(key, exchange_currencyPair_meta);
			}
			get_exchange_meta_timer = 0;
		} else {
			get_exchange_meta_timer++;
		}

		for (String key : tradable_pairs.keySet()) {
			String[] exchanges_keys = key.split(":");
			for (CurrencyPair cp : tradable_pairs.get(key)) {
				int isValid = 0;

				// System.out.println(key.toString());
				// System.out.print(cp.toString()+" ");
				try {
					if (all_tickers.get(exchanges_keys[0]).containsKey(cp)) {
						// System.out.print(exchanges_keys[0]+": ");
						// Ticker tick = all_tickers.get(exchanges_keys[0]).get(cp);
						// System.out.print("SELL "+tick.getAsk().toString()+" BUY
						// "+tick.getBid().toString());
						// System.out.print(" - ");
						isValid++;
					}
					if (all_tickers.get(exchanges_keys[1]).containsKey(cp)) {
						// System.out.print(exchanges_keys[1]+": ");
						// Ticker tick = all_tickers.get(exchanges_keys[1]).get(cp);
						// System.out.print("SELL "+tick.getAsk().toString()+" BUY
						// "+tick.getBid().toString());
						isValid++;
					}

					if (isValid == 2) {
						Ticker tick0 = all_tickers.get(exchanges_keys[0]).get(cp);
						Ticker tick1 = all_tickers.get(exchanges_keys[1]).get(cp);

						Ticker lowest_buy = null;
						Ticker highest_sell = null;
						String lowest_buy_exchange = null;
						String highest_sell_exchange = null;

						// lowest buy
						if (tick0.getBid().compareTo(tick1.getBid()) <= 0) {
							lowest_buy = tick0;
							lowest_buy_exchange = exchanges_keys[0];
						} else {
							lowest_buy = tick1;
							lowest_buy_exchange = exchanges_keys[1];
						}

						// highest sell
						if (tick0.getAsk().compareTo(tick1.getAsk()) >= 0) {
							highest_sell = tick0;
							highest_sell_exchange = exchanges_keys[0];
						} else {
							highest_sell = tick1;
							highest_sell_exchange = exchanges_keys[1];
						}

						if (lowest_buy_exchange != highest_sell_exchange) {
							try {
								PotentialPair ppair = new PotentialPair();
								ppair.SetBuyTicker(lowest_buy_exchange, lowest_buy);
								ppair.SetSellTicker(highest_sell_exchange, highest_sell);

								ppair.SetCurrencyPair(cp);

								ppair.SetMinTradeAmmountBuy(
										all_currencypair_metadata.get(lowest_buy_exchange).get(cp).getMinimumAmount());
								ppair.SetTradeFeeBuy(
										all_currencypair_metadata.get(lowest_buy_exchange).get(cp).getTradingFee());
								ppair.SetWithdrawBuyFee(
										all_currency_metadata.get(lowest_buy_exchange).get(cp.base).getWithdrawalFee());

								ppair.SetMinTradeAmmountSell(all_currencypair_metadata.get(highest_sell_exchange)
										.get(cp).getMinimumAmount());
								ppair.SetTradeFeeSell(
										all_currencypair_metadata.get(highest_sell_exchange).get(cp).getTradingFee());
								ppair.SetWithdrawSellFee(all_currency_metadata.get(highest_sell_exchange)
										.get(cp.counter).getWithdrawalFee());

								double delta_profit = 0;
								double delta_profit_procent = 0;
								double buy_withdraw_fee = all_currency_metadata.get(lowest_buy_exchange).get(cp.base)
										.getWithdrawalFee().doubleValue();
								double sell_withdraw_fee = all_currency_metadata.get(highest_sell_exchange)
										.get(cp.counter).getWithdrawalFee().doubleValue();
								double buy_fee = all_currencypair_metadata.get(lowest_buy_exchange).get(cp)
										.getTradingFee().doubleValue();
								if (lowest_buy_exchange.equals("poloniex")) {
									buy_fee = buy_fee * 100;
								}
								double sell_fee = all_currencypair_metadata.get(highest_sell_exchange).get(cp)
										.getTradingFee().doubleValue();
								if (highest_sell_exchange.equals("poloniex")) {
									sell_fee = sell_fee * 100;
								}
								double buy_price = lowest_buy.getBid().doubleValue();
								buy_price = buy_price + ((contingent_procent / 100) * buy_price);
								double sell_price = highest_sell.getAsk().doubleValue();
								sell_price = sell_price - ((contingent_procent / 100) * sell_price);

								double buy_ammount = dollar_ammount / lowest_buy.getLast().doubleValue();
								// System.out.println("Buy ammount is "+buy_ammount);

								double buy_counter_cost = buy_ammount * buy_price;
								double bought_ammount = buy_ammount - (buy_ammount * (buy_fee / 100));

								double sell_ammount = bought_ammount - buy_withdraw_fee;
								double sell_counter_result = sell_ammount * sell_price;
								sell_counter_result = sell_counter_result - (sell_counter_result * (sell_fee / 100));
								double swapped_counter = sell_counter_result - sell_withdraw_fee;
								double to_be_swapped_counter = swapped_counter
										- ((swapped_counter - buy_counter_cost) / 2);

								if (swapped_counter < buy_counter_cost) {
									// negative profit
									delta_profit = buy_counter_cost - swapped_counter;
									delta_profit_procent = (delta_profit * 100) / buy_counter_cost;
									delta_profit_procent *= -1;
									delta_profit *= -1;
								} else {
									// positive profit
									delta_profit = swapped_counter - buy_counter_cost;
									delta_profit_procent = (delta_profit * 100) / buy_counter_cost;
								}

								ppair.SetMisc(" ");
								// System.out.print(cp.toString()+" buy "+lowest_buy_exchange+" ");
								// System.out.print("buy ammount "+buy_ammount+" "+cp.base.toString()+" bought
								// ammount "+bought_ammount+" "+cp.base.toString()+" ");
								// System.out.print(" buy counter cost "+buy_counter_cost+"
								// "+cp.counter.toString()+" sell "+highest_sell_exchange+" ");
								// System.out.print(" sell_ammount "+sell_ammount+" "+cp.base.toString()+" sell
								// eth result "+sell_counter_result+" "+cp.counter.toString()+" ");
								// System.out.print("swapped counter "+swapped_counter+" to be swapped
								// "+to_be_swapped_counter+" "+cp.counter.toString()+" delta_profit
								// "+delta_profit+" "+cp.counter.toString()+" ");
								// System.out.println(" delta profit procent "+delta_profit_procent+" %");
								//

								if (delta_profit_procent > potential_delta_profit_procent) {
									///
									Date last_buy_tstmp = null;
									Date last_sell_tstmp = null;
									System.out.println(cp.toString() + " ############### positive profit "
											+ delta_profit_procent + " %");
									ppair.SetBuyReq(lowest_buy_exchange, new BigDecimal(buy_price),
											new BigDecimal(buy_ammount), new BigDecimal(bought_ammount));
									ppair.SetSellReq(highest_sell_exchange, new BigDecimal(sell_price),
											new BigDecimal(sell_ammount), new BigDecimal(to_be_swapped_counter));

									ppair.SetMisc(cp.toString() + " buy " + lowest_buy_exchange + " " + "buy ammount "
											+ buy_ammount + " " + cp.base.toString() + " bought  ammount "
											+ bought_ammount + " " + cp.base.toString() + " " + " buy counter cost "
											+ buy_counter_cost + " " + cp.counter.toString() + " sell "
											+ highest_sell_exchange + " " + " sell_ammount " + sell_ammount + " "
											+ cp.base.toString() + " sell eth result " + sell_counter_result + " "
											+ cp.counter.toString() + " " + "swapped counter " + swapped_counter
											+ " to be swapped " + to_be_swapped_counter + " " + cp.counter.toString()
											+ " delta_profit " + delta_profit + " " + cp.counter.toString() + " "
											+ " delta profit procent " + delta_profit_procent + " %");

									ppair.SetDeltaProcent(delta_profit_procent);

									// check that the last trade was not so long ago on any exchange
									try {
										Trades t = exchanges.get(lowest_buy_exchange).getMarketDataService()
												.getTrades(cp);
										// System.out.println(t.toString());
										Iterator<Trade> itr = t.getTrades().iterator();
										Trade tr = null;
										while (itr.hasNext()) {
											tr = itr.next();
										}
										if (tr == null) {
											continue;
										}
										Date tstmp = tr.getTimestamp();
										last_buy_tstmp = tstmp;
										Date now = new Date();

										// if the last trade was to long ago..more than seconds jump over this pair
										if ((now.getTime() - tstmp.getTime()) > last_trade_delay) {
											System.out.println(tr);
											System.out.println(
													now.toString() + " " + tstmp.toString() + " " + tstmp.getTime());
											System.out.println("Last trade was long time ago " + lowest_buy_exchange
													+ " " + (now.getTime() - tstmp.getTime()));
											continue;
										}
									} catch (IOException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									} catch (ExchangeException e) {
										e.printStackTrace();
										System.err.println(e.getMessage());
									}

									try {
										Trades t = exchanges.get(highest_sell_exchange).getMarketDataService()
												.getTrades(cp);
										// System.out.println(t.toString());
										Iterator<Trade> itr = t.getTrades().iterator();
										Trade tr = null;
										while (itr.hasNext()) {
											tr = itr.next();
										}
										if (tr == null) {
											continue;
										}
										Date tstmp = tr.getTimestamp();
										last_sell_tstmp = tstmp;
										Date now = new Date();

										// if the last trade was to long ago..more than seconds jump over this pair
										if ((now.getTime() - tstmp.getTime()) > last_trade_delay) {
											System.out.println(tr);
											System.out.println("Last trade was long time ago " + highest_sell_exchange
													+ " " + (now.getTime() - tstmp.getTime()));
											continue;
										}

										ppair.SetLastTradeTstmp(last_buy_tstmp, last_sell_tstmp);
									} catch (IOException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									} catch (ExchangeException e) {
										e.printStackTrace();
										System.err.println(e.getMessage());
									}

									ppair_list.add(ppair);
								} else {
									System.out.println(cp.toString() + " ############### negative profit "
											+ delta_profit_procent + " %  " + key);
								}
							} catch (NullPointerException e) {
								// e.printStackTrace();
							}
						}
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				// System.out.println();
			}
		}
	}

	public static PotentialPair getMaxPotentialPair() {
		// max delta procent
		System.out.println("LIST of potential PAIRS : ");
		PotentialPair max_delta_procent_ppair = null;
		if (ppair_list.size() > 0) {
			max_delta_procent_ppair = ppair_list.get(0);
		}
		for (PotentialPair ppair : ppair_list) {
			System.out.println(ppair);
			ppair.InsertPotentialPair(conn, positive_pairs, ppair);
			if (max_delta_procent_ppair.GetDeltaProcent() < ppair.GetDeltaProcent()) {
				max_delta_procent_ppair = ppair;
			}
		}
		System.out.println("The potential pair with highest delta is: \n" + max_delta_procent_ppair);

		return max_delta_procent_ppair;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		connectDB();

	
		init();

		connectExchanges();

		setSymbols();		
		
		state = getStateMachine();
		while (true) {
			System.out.println("Where are in state " + state.toString());
			switch (state) {
			case INIT:
				changeState(STATE.SEARCH_POTENTIAL);
				break;
			case SEARCH_POTENTIAL:
				transact_ppair = null;
				getTickers();

				generatePotentialPairs();

				transact_ppair = getMaxPotentialPair();
				
				if (transact_ppair == null) 
				{
					try {
						Thread.sleep(loop_delay);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					// request execute transaction
					changeState(STATE.REQ_EXECUTE_TRANSACTION);
				}
				break;
			case REQ_EXECUTE_TRANSACTION:
				// req_execute_transaction();
				changeState(STATE.PENDING_EXECUTED_TRANSACTION);
				break;
			case PENDING_EXECUTED_TRANSACTION:
				changeState(STATE.EXECUTED_TRANSACTION);
				break;
			case EXECUTED_TRANSACTION:
				changeState(STATE.REQUEST_SWAP);
				// req_execute_transaction();
				break;
			case REQUEST_SWAP:
				changeState(STATE.PENDING_SWAP);
				// req_execute_transaction();
				break;
			case PENDING_SWAP:
				changeState(STATE.EXECUTED_SWAP);
				break;
			case EXECUTED_SWAP:
				changeState(STATE.SEARCH_POTENTIAL);
				break;
			default:
				System.err.println("Unknown state " + state.toString());
				System.exit(-1);
			}

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
