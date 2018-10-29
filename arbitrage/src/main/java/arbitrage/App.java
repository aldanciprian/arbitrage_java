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
import org.knowm.xchange.dto.meta.ExchangeMetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;





public class App {

    public static List<String> exchangesNames = null;
    public static Map<String,List<CurrencyPair>> tradable_pairs = null;
    public static Map<String,Exchange> exchanges = null;
    public static Map<String,List<CurrencyPair>> pairPerExchange = null;
    public static Map<String,Map<CurrencyPair,Ticker>> all_tickers = null;
    public static List<PotentialPair> ppair_list =  null;
    public static Map<String,List<CurrencyPair>> all_eth_symbols = null;
    public static Map<String,List<CurrencyPair>> all_filters =  null;
    
    
    public static Connection conn = null;
    public static String positive_pairs = "positive_pairs";
    

    public static void connectDB()
    {
        try {
            // Step 1: Allocate a database 'Connection' object
        	String query="";
        	String cols = "";
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/arbitrage", "ciprian", "ciprian");
            // MySQL: "jdbc:mysql://hostname:port/databaseName", "username", "password"

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
            
            query +=  "create table if not exists "+positive_pairs+" ( "+cols +" );";
            
//            System.out.println(query);
            
            // Step 2: Allocate a 'Statement' object in the Connection
            Statement stmt = conn.createStatement();

            stmt.execute(query);

        } catch(SQLException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static void connectExchanges() {
        for ( String s: exchangesNames)
        {
            ExchangeSpecification spec = null;
            switch (s)
            {
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

            spec.setApiKey(System.getenv(s+"_APIKEY"));
            spec.setSecretKey(System.getenv(s+"_SECRET"));
            exchanges.put(s, ExchangeFactory.INSTANCE.createExchange(spec));
        }
    }

    public static void init()
    {
        connectDB();

        tradable_pairs = new HashMap<String,List<CurrencyPair>>();


        exchangesNames  =  new Vector<String>();
        exchangesNames.add("bitfinex");
        exchangesNames.add("binance");
        exchangesNames.add("poloniex");


        ppair_list = new Vector<PotentialPair>();

        exchanges = new HashMap<String,Exchange>();

        all_tickers = new  HashMap<String,Map<CurrencyPair,Ticker>>();

        all_eth_symbols = new HashMap<String,List<CurrencyPair>>();

        all_filters = new HashMap<String,List<CurrencyPair>>();
        
        // Open the file
        try 
        {
            FileInputStream fstream = new FileInputStream("src/main/resources/whitepairs.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
              // Print the content on the console
//              System.out.println (strLine);
              if ( strLine.contains("="))
              {
                  String[] parts = strLine.split("=");
                  String[] base_counter = parts[1].split("/");
                  if ( all_filters.containsKey(parts[0]))
                  {
                	  all_filters.get(parts[0]).add(new CurrencyPair(base_counter[0],base_counter[1]));
                  }
                  else
                  {
                	  List<CurrencyPair> pairs = new Vector<CurrencyPair>();
                	  pairs.add(new CurrencyPair(base_counter[0],base_counter[1]));
                	  all_filters.put(parts[0],pairs);                	  
                  }
              }
            }
            
//            Set<String> keys = all_filters.keySet();
//            for ( String key :  keys)
//            {
//            	System.out.print(key+" ");
//            	for ( CurrencyPair cp : all_filters.get(key)) {
//            		System.out.print(cp.toString()+",");
//            	}
//            	System.out.println();
//            }

            //Close the input stream
            br.close();
        } catch (Exception e)
        {
        	e.printStackTrace();
        }
        
//        System.exit(0);
        
    }

    public static void setSymbols()
    {
        for ( String key: exchangesNames )
        {
            List<CurrencyPair> eth_symbols = new Vector<CurrencyPair>();
            List<CurrencyPair> symbols = exchanges.get(key).getExchangeSymbols();

            for (CurrencyPair cp : symbols)
            {
            	// check if it in white list
            	boolean isWhite =  false;
            	for ( CurrencyPair white_cp : all_filters.get(key)) {
            		if  ( cp.compareTo(white_cp) == 0)
            		{
            			isWhite = true;
            			break;
            		}
            	}
            	
//            	if ( isWhite == true)  
            	{
                    if ( (cp.counter == Currency.ETH) /*|| (cp.base == Currency.ETH)*/ )
                    {

                        eth_symbols.add(cp);
                    }
            	}
            }
            all_eth_symbols.put(key, eth_symbols);
        }
        for ( String key: exchangesNames )
        {
            for ( String key2: exchangesNames )
            {
                if ( key != key2 )
                {
                    List<CurrencyPair> common_symbols = new Vector<CurrencyPair>();
                    for ( CurrencyPair elem: all_eth_symbols.get(key) )
                    {
                        for ( CurrencyPair elem2: all_eth_symbols.get(key2) )
                        {
                            if ( elem.compareTo(elem2) == 0 )
                            {
                                common_symbols.add(elem);
                                break;
                            }
                        }
                    }
                    if ( !tradable_pairs.containsKey(key+":"+key2) && !tradable_pairs.containsKey(key2+":"+key) )
                    {
                        tradable_pairs.put(key+":"+key2,common_symbols);
                    }
                }
            }
        }


        pairPerExchange = new HashMap<String,List<CurrencyPair>>();
        for (String key : tradable_pairs.keySet())
        {
            String[] exchanges_keys = key.split(":");
            for ( CurrencyPair cp : tradable_pairs.get(key))
            {
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
                    if ( contains == false )
                    {
                        pairPerExchange.get(exchanges_keys[0]).add(cp);
                    }
                } else {
//					System.out.println(exchanges_keys[0]+"  adding to list");
                    List<CurrencyPair> list_cp = new Vector<CurrencyPair>();
                    list_cp.add(cp);
                    pairPerExchange.put(exchanges_keys[0], list_cp);
                }
            }


            for ( CurrencyPair cp : tradable_pairs.get(key))
            {
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
                    if ( contains == false )
                    {
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


    public static void getTickers()
    {
        all_tickers.clear();
        List<Thread> workers = new Vector<Thread>();
        for ( String key: exchangesNames )
        {
            MyRunnable r = new MyRunnable(key);
            Thread th = new Thread(r);
            workers.add(th);

            th.start();
        }

        for ( Thread th : workers)
        {
            try {
                th.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void generatePotentialPairs()
    {
        ppair_list.clear();

        for (String key : tradable_pairs.keySet())
        {
            String[] exchanges_keys = key.split(":");
            for ( CurrencyPair cp : tradable_pairs.get(key))
            {
                int isValid = 0;
                
//				System.out.println(key.toString());
//				System.out.print(cp.toString()+"  ");
                try
                {
                    if (all_tickers.get(exchanges_keys[0]).containsKey(cp))
                    {
//						System.out.print(exchanges_keys[0]+": ");
//						Ticker tick = all_tickers.get(exchanges_keys[0]).get(cp);
//						System.out.print("SELL "+tick.getAsk().toString()+"  BUY "+tick.getBid().toString());
//						System.out.print("  -  ");
                        isValid++;
                    }
                    if (all_tickers.get(exchanges_keys[1]).containsKey(cp))
                    {
//						System.out.print(exchanges_keys[1]+": ");
//						Ticker tick = all_tickers.get(exchanges_keys[1]).get(cp);
//						System.out.print("SELL "+tick.getAsk().toString()+"  BUY "+tick.getBid().toString());
                        isValid++;
                    }

                    if ( isValid == 2)
                    {
                        Ticker tick0 = all_tickers.get(exchanges_keys[0]).get(cp);
                        Ticker tick1 = all_tickers.get(exchanges_keys[1]).get(cp);

                        Ticker lowest_buy = null;
                        Ticker highest_sell = null;
                        String lowest_buy_exchange = null;
                        String highest_sell_exchange = null;

                        // lowest buy
                        if (  tick0.getBid().compareTo(tick1.getBid())  <= 0  )
                        {
                            lowest_buy = tick0;
                            lowest_buy_exchange = exchanges_keys[0];
                        }
                        else
                        {
                            lowest_buy = tick1;
                            lowest_buy_exchange = exchanges_keys[1];
                        }

                        // highest sell
                        if (  tick0.getAsk().compareTo(tick1.getAsk())  >= 0  )
                        {
                            highest_sell = tick0;
                            highest_sell_exchange = exchanges_keys[0];
                        }
                        else
                        {
                            highest_sell = tick1;
                            highest_sell_exchange = exchanges_keys[1];
                        }

                        if ( lowest_buy_exchange != highest_sell_exchange)
                        {
                            
                            // check that the last trade was not so long ago on any exchange
                        	try {
								Trades  t = exchanges.get(lowest_buy_exchange).getMarketDataService().getTrades(cp, 10);
//								System.out.println(t.toString());
								Iterator<Trade> itr  = t.getTrades().iterator();
								Trade tr = null;
								while  (  itr.hasNext() )
								{
									tr=itr.next();
								}
								if ( tr == null )
								{
									continue;
								}
								Date tstmp = tr.getTimestamp();
								Date now = new Date();
								
								// if the last trade was to long ago..more than 60 seconds jump over this pair
								if ( ( now.getTime() - tstmp.getTime() ) > 60000)
								{
									System.out.println(tr);
									System.out.println(now.toString()+" "+tstmp.toString()+" "+tstmp.getTime());
									System.out.println("Last trade was long time ago "+lowest_buy_exchange+" "+( now.getTime() - tstmp.getTime() ));
									continue;
								}
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
                        	
                            
                        	try {
								Trades  t = exchanges.get(highest_sell_exchange).getMarketDataService().getTrades(cp, 10);
//								System.out.println(t.toString());
								Iterator<Trade> itr  = t.getTrades().iterator();
								Trade tr = null;
								while  (  itr.hasNext() )
								{
									tr=itr.next();
								}
								if ( tr == null )
								{
									continue;
								}
								Date tstmp = tr.getTimestamp();
								Date now = new Date();
								
								// if the last trade was to long ago..more than 60 seconds jump over this pair
								if ( ( now.getTime() - tstmp.getTime() ) > 60000)
								{
									System.out.println(tr);
									System.out.println("Last trade was long time ago "+highest_sell_exchange+" "+( now.getTime() - tstmp.getTime() ));
									continue;
								}
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
                        	
                        	
                        	
                        	
                        	
                            try
                            {
                                ExchangeMetaData ex_meta_buy = exchanges.get(lowest_buy_exchange).getExchangeMetaData();
                                ExchangeMetaData ex_meta_sell = exchanges.get(highest_sell_exchange).getExchangeMetaData();

                                PotentialPair ppair = new PotentialPair();
                                ppair.SetBuyTicker(lowest_buy_exchange, lowest_buy);
                                ppair.SetSellTicker(highest_sell_exchange, highest_sell);

                                ppair.SetCurrencyPair(cp);

                                ppair.SetMinTradeAmmountBuy(ex_meta_buy.getCurrencyPairs().get(cp).getMinimumAmount());
                                ppair.SetTradeFeeBuy(ex_meta_buy.getCurrencyPairs().get(cp).getTradingFee());
                                ppair.SetWithdrawBuyFee(ex_meta_buy.getCurrencies().get(cp.base).getWithdrawalFee());


                                ppair.SetMinTradeAmmountSell(ex_meta_sell.getCurrencyPairs().get(cp).getMinimumAmount());
                                ppair.SetTradeFeeSell(ex_meta_sell.getCurrencyPairs().get(cp).getTradingFee());
                                ppair.SetWithdrawSellFee(ex_meta_sell.getCurrencies().get(cp.counter).getWithdrawalFee());

                                //simulate for 100 base units
                                double delta_profit = 0;
                                double delta_profit_procent = 0;
                                double contingent_procent = 0;
                                double buy_withdraw_fee = ex_meta_buy.getCurrencies().get(cp.base).getWithdrawalFee().doubleValue();
                                double sell_withdraw_fee = ex_meta_sell.getCurrencies().get(cp.counter).getWithdrawalFee().doubleValue();
                                double buy_fee = ex_meta_buy.getCurrencyPairs().get(cp).getTradingFee().doubleValue();
                                if ( lowest_buy_exchange.equals("poloniex"))
                                {
                                	buy_fee = buy_fee * 100;
                                }
                                double sell_fee = ex_meta_sell.getCurrencyPairs().get(cp).getTradingFee().doubleValue();
                                if ( highest_sell_exchange.equals("poloniex"))
                                {
                                	sell_fee = sell_fee * 100;
                                }
                                double buy_price = lowest_buy.getBid().doubleValue();
                                buy_price = buy_price + ((contingent_procent/100)*buy_price);
                                double sell_price = highest_sell.getAsk().doubleValue();
                                sell_price = sell_price - ((contingent_procent/100)*sell_price);
                                
                                double dollar_ammount  = 0.00469;  // aprox 30 $
                                CurrencyPair dolar_pair = new CurrencyPair(cp.base,Currency.BTC);
//                                System.out.println(dolar_pair.toString());
                                Ticker base_dollar = exchanges.get(lowest_buy_exchange).getMarketDataService().getTicker(dolar_pair);
                                
                                double simulation_ammount = dollar_ammount / base_dollar.getLast().doubleValue();
                                double buy_ammount = simulation_ammount;
                                double buy_eth_cost = buy_ammount * buy_price;
                                double bought_ammount = buy_ammount - (buy_ammount*(buy_fee/100));
                                
                                double sell_ammount = bought_ammount - buy_withdraw_fee;
                                double sell_eth_result = sell_ammount * sell_price;
                                sell_eth_result = sell_eth_result - (sell_eth_result*(sell_fee/100));
                                double swapped_eth = sell_eth_result - sell_withdraw_fee;
                                double to_be_swapped_eth = swapped_eth - ((swapped_eth - buy_eth_cost) /2);
                                
                                if ( swapped_eth < buy_eth_cost)
                                {
                                	// negative profit
                                	delta_profit = buy_eth_cost - swapped_eth;
                                	delta_profit_procent = ( delta_profit * 100) / buy_eth_cost;
                                	delta_profit_procent *= -1;
                                	delta_profit *= -1;
                                }
                                else
                                {
                                  	// positive profit
                                	delta_profit = swapped_eth - buy_eth_cost;
                                	delta_profit_procent = ( delta_profit * 100) / buy_eth_cost;
                                }
                                
                                
                                ppair.SetMisc(" ");
//                                System.out.print(cp.toString()+" buy "+lowest_buy_exchange+" ");
//                                System.out.print("buy ammount "+buy_ammount+" "+cp.base.toString()+" bought  ammount "+bought_ammount+" "+cp.base.toString()+" ");
//                                System.out.print(" buy eth cost "+buy_eth_cost+" "+cp.counter.toString()+" sell "+highest_sell_exchange+" ");
//                                System.out.print(" sell_ammount "+sell_ammount+" "+cp.base.toString()+" sell eth result "+sell_eth_result+" "+cp.counter.toString()+" ");
//                                System.out.print("swapped eth "+swapped_eth+" to be swapped "+to_be_swapped_eth+" "+cp.counter.toString()+" delta_profit "+delta_profit+" "+cp.counter.toString()+" ");
//                                System.out.println(" delta profit procent "+delta_profit_procent+" %");
//


                                if ( delta_profit > 0)
                                {
                                	///
                                	System.out.println(cp.toString()+ " ############### positive profit "+delta_profit_procent+" %");
                                	ppair.SetBuyReq(lowest_buy_exchange,new BigDecimal(buy_price) ,new BigDecimal(buy_ammount),new BigDecimal(bought_ammount));
                                	ppair.SetSellReq(highest_sell_exchange,new BigDecimal(sell_price) ,new BigDecimal(sell_ammount),new BigDecimal(to_be_swapped_eth));
                                	
                                    ppair.SetMisc(cp.toString()+" buy "+lowest_buy_exchange+" "+
                                    		"buy ammount "+buy_ammount+" "+cp.base.toString()+" bought  ammount "+bought_ammount+" "+cp.base.toString()+" "+
                                    		" buy eth cost "+buy_eth_cost+" "+cp.counter.toString()+" sell "+highest_sell_exchange+" "+
                                    		" sell_ammount "+sell_ammount+" "+cp.base.toString()+" sell eth result "+sell_eth_result+" "+cp.counter.toString()+" "+
                                    		"swapped eth "+swapped_eth+" to be swapped "+to_be_swapped_eth+" "+cp.counter.toString()+" delta_profit "+delta_profit+" "+cp.counter.toString()+" "+
                                    		" delta profit procent "+delta_profit_procent+" %"
                                    		);

                                    ppair.SetDeltaProcent(delta_profit_procent);                                    
                                    
                                    ppair_list.add(ppair);                                	
                                }
                                else
                                {
                                	System.out.println(cp.toString()+ " ############### negative profit "+delta_profit_procent+" %");
                                }
                            } catch ( NullPointerException e)
                            {
                              //  e.printStackTrace();
                            } catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                        }
                    }
                } catch (NullPointerException e)
                {
                    e.printStackTrace();
                }
//				System.out.println();
            }
        }
    }

    public static PotentialPair getMaxPotentialPair()
    {
        // max delta procent
    	System.out.println("LIST of potential PAIRS : ");
        PotentialPair max_delta_procent_ppair=  null;
        if ( ppair_list.size() > 0 )
        {
            max_delta_procent_ppair = ppair_list.get(0);
        }
        for ( PotentialPair ppair: ppair_list)
        {
            System.out.println(ppair);
//            ppair.InsertPotentialPair(conn,positive_pairs,ppair);
            if ( max_delta_procent_ppair.GetDeltaProcent() < ppair.GetDeltaProcent() )
            {
                max_delta_procent_ppair = ppair;
            }
        }
        System.out.println("The potential pair with highest delta is: \n"+ max_delta_procent_ppair);

        return max_delta_procent_ppair;
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub


   	
        init();

        connectExchanges();

        setSymbols();

        while ( true )
        {
            getTickers();

            generatePotentialPairs();

            getMaxPotentialPair();

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}
