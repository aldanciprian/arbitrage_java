package arbitrage;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;


public class PotentialPair {
	
	
	Map<String,Ticker> buy_ticker;
	Map<String,Ticker> sell_ticker;
	double delta_procent;
	CurrencyPair cp;
	BigDecimal min_trade_ammount_buy;
	BigDecimal trade_fee_buy;
	BigDecimal min_trade_ammount_sell;
	BigDecimal trade_fee_sell;
	BigDecimal withdraw_fee_buy;
	BigDecimal withdraw_fee_sell;
	
	String buy_exchange;
	BigDecimal buy_price;
	BigDecimal buy_ammount;
	BigDecimal swap_buy_ammount;
	
	String sell_exchange;
	BigDecimal sell_price;
	BigDecimal sell_ammount;
	BigDecimal swap_sell_ammount;
	
	String misc;
	
	public PotentialPair()
	{
		buy_ticker = new HashMap<String,Ticker>();
		sell_ticker = new HashMap<String,Ticker>();
		delta_procent = 0 ;
	}
	
	public void SetBuyReq(String exchange,BigDecimal price,BigDecimal ammount,BigDecimal swap_ammount)
	{
		buy_exchange = exchange;
		buy_price = price;
		buy_ammount = ammount;
		swap_buy_ammount = swap_ammount;
	}

	public void SetSellReq(String exchange,BigDecimal price,BigDecimal ammount,BigDecimal swap_ammount)
	{
		sell_exchange = exchange;
		sell_price = price;
		sell_ammount = ammount;
		swap_sell_ammount = swap_ammount;
	}	
	
	public void SetMisc(String _misc)
	{
		misc = _misc;
	}
	public void SetWithdrawBuyFee(BigDecimal fee)
	{
		withdraw_fee_buy = new BigDecimal(fee.doubleValue());
	}
	public void SetWithdrawSellFee(BigDecimal fee)
	{
		withdraw_fee_sell = new BigDecimal(fee.doubleValue());
	}
	public void SetMinTradeAmmountBuy(BigDecimal ammount)
	{
		min_trade_ammount_buy = new BigDecimal(ammount.doubleValue());
	}
	public void SetTradeFeeBuy(BigDecimal fee)
	{
		 trade_fee_buy = new BigDecimal(fee.doubleValue());
	}
	public void SetMinTradeAmmountSell(BigDecimal ammount)
	{
		min_trade_ammount_sell = new BigDecimal(ammount.doubleValue());
	}
	public void SetTradeFeeSell(BigDecimal fee)
	{
		 trade_fee_sell = new BigDecimal(fee.doubleValue());
	}
	public void SetBuyTicker(String exchange,Ticker tick)
	{
		buy_ticker.put(exchange, tick);
	}
	public void SetSellTicker(String exchange,Ticker tick)
	{
		sell_ticker.put(exchange, tick);
	}
	public void SetDeltaProcent(double proc)
	{
		delta_procent = proc;
	}
	public void SetCurrencyPair(CurrencyPair pair)
	{
		cp = new CurrencyPair(pair.toString());
	}
	public BigDecimal GetMinTradeAmmountBuy()
	{
		return min_trade_ammount_buy;
	}
	public BigDecimal GetTradeFeeBuy()
	{
		return trade_fee_buy;
	}
	public BigDecimal GetMinTradeAmmountSell()
	{
		return min_trade_ammount_sell;
	}
	public BigDecimal GetTradeFeeSell()
	{
		return trade_fee_sell;
	}
	public BigDecimal GetWithdrawBuyFee()
	{
		return withdraw_fee_buy ;
	}
	public BigDecimal GetWithdrawSellFee()
	{
		return withdraw_fee_sell ;
	}
	public Ticker GetBuyTicker() 
	{
		return buy_ticker.get(buy_exchange);
	}
	public String GetBuyExchange()
	{
		return buy_exchange;
	}
	public Ticker GetSellTicker() 
	{
		return sell_ticker.get(sell_exchange);
	}
	public String GetSellExchange()
	{
		return sell_exchange;
	}	
	public double GetDeltaProcent()
	{
		return delta_procent;
	}
	public String GetMisc()
	{
		return misc;
	}
	
	public CurrencyPair GetCurrencyPair()
	{
		return cp;
	}
	
	
	public BigDecimal GetBuyPrice()
	{
		return buy_price;
	}
	public BigDecimal GetBuyAmmount()
	{
		return buy_ammount;
	}
	public BigDecimal GetBuySwapAmmount()
	{
		return swap_buy_ammount;
	}
	
	public BigDecimal GetSellPrice()
	{
		return sell_price;
	}
	public BigDecimal GetSellAmmount()
	{
		return sell_ammount;
	}
	public BigDecimal GetSellSwapAmmount()
	{
		return swap_sell_ammount;
	}
	
	public void InsertPotentialPair(Connection conn,String table,PotentialPair ppair)
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String query="";
		String cols="";
		
		cols += "'"+timestamp.toString()+"'";
		cols += ",";
		cols += "'"+ppair.cp.toString()+"'";
		cols += ",";
		cols += "'"+ppair.buy_exchange.toString()+"'";
		cols += ",";
		cols += BigDecimal.valueOf(ppair.buy_price.doubleValue());		
		cols += ",";
		cols += "'"+ppair.sell_exchange.toString()+"'";		
		cols += ",";
		cols += BigDecimal.valueOf(ppair.sell_price.doubleValue());
		cols += ",";
		cols += BigDecimal.valueOf(ppair.delta_procent);		
		
		
		
		query = "insert into "+table+" values ("+cols+")";
		System.out.println(query);
		
	      // Step 2: Allocate a 'Statement' object in the Connection
        Statement stmt;
		try {
			stmt = conn.createStatement();
	        stmt.execute(query);			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public String toString() {
		return cp.toString()+"  BUY  "+GetBuyExchange()+"  "+
					GetBuyTicker().getBid().toString()+"  SELL  "+GetSellExchange()+"  "+GetSellTicker().getAsk().toString()+"  - "
				   +delta_procent+" % "+" buy fee "+trade_fee_buy.doubleValue()+" sell fee "+trade_fee_sell.doubleValue()+" min trade ammount buy "+min_trade_ammount_buy.doubleValue()+
				   " min trade ammount sell "+min_trade_ammount_sell.doubleValue()+" withdraw fee buy "+withdraw_fee_buy.doubleValue()+" - withdraw fee sell "+withdraw_fee_sell.doubleValue()+"\n"+misc+" \n"+
				   " BUY "+ buy_exchange+" "+buy_price.doubleValue()+" "+buy_ammount.doubleValue()+" "+swap_buy_ammount.doubleValue()+" --- "+
				   " SELL "+ sell_exchange+" "+sell_price.doubleValue()+" "+sell_ammount.doubleValue()+" "+swap_sell_ammount.doubleValue();
	}

}
