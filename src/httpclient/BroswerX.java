package httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
/**
 * 一个简陋的模拟登录教务系统应用
 * @author hxp
 *
 */
public class BroswerX {
	DefaultHttpClient client;
	String mainUrl;
	String sessionID;
	String username;
	String password;
	String url = "http://jwgldx.gdut.edu.cn/";
	String loginUrl;
	String checkImgUrl = "http://jwgldx.gdut.edu.cn/(imgurl)/CheckCode.aspx";
	String viewstate;
	String stuName;
	String panelUrl;
	String getStateUrl;
	String getGradeUrl;
	List<NameValuePair> params = new ArrayList<NameValuePair>();
/**
 * 初始化登录信息
 * @param username 用户名
 * @param password 密码	
 * @throws Exception
 */
	public BroswerX(String username, String password) throws Exception {
		this.client = new DefaultHttpClient();
		this.username = username;
		this.password = password;
		this.setSessionId();
		System.out.println(this.sessionID);
		System.out.println(this.checkImgUrl);

		this.doLogin();

	}
/**
 * 设置登录状态
 * @throws Exception
 */
	public void setSessionId() throws Exception {
		HttpResponse httpResponse;
		HttpPost post = new HttpPost();
		post.setURI(new URI(this.url));
		httpResponse = client.execute(post);

		int statecode = httpResponse.getStatusLine().getStatusCode();
		if (statecode == 302) {
			Header[] headers = httpResponse.getHeaders("Location");
			for (Header header : headers) {
				String redirectuUri = header.getValue();
				if (redirectuUri.contains("(")) {
					this.sessionID = redirectuUri.substring(
							redirectuUri.indexOf("(") + 1,
							redirectuUri.indexOf(")"));
					this.checkImgUrl = this.checkImgUrl.replace("imgurl",
							this.sessionID);
					this.mainUrl = url + "(" + this.sessionID + ")/";
					this.getGradeUrl = this.mainUrl + "xscj.aspx?xh="
							+ this.username + "&gnmkdm=N121605";
				}
			}
		}
		post.abort();
	}
/**
 * 登录
 * @return
 * @throws Exception
 */
	public String doLogin() throws Exception {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("RadioButtonList1", "学生"));
		params.add(new BasicNameValuePair("TextBox2", password));
		params.add(new BasicNameValuePair("__VIEWSTATE",
				"dDwyMDczNjQ0MDAyOzs+qf+pmRG/1ZheVN82pOy8rkB3ymA="));
		params.add(new BasicNameValuePair("txtUserName", username));
		params.add(new BasicNameValuePair("txtSecretCode", ""));
		params.add(new BasicNameValuePair("Button1", ""));
		params.add(new BasicNameValuePair("hidPdrs", ""));
		params.add(new BasicNameValuePair("hidsc", ""));
		params.add(new BasicNameValuePair("lbLanguage", ""));
		
		String loginref = url + "(" + sessionID + ")" + "/default2.aspx";
		this.loginUrl = loginref;
		HttpPost loginPost = new HttpPost(loginUrl);
		loginPost.setEntity(new UrlEncodedFormEntity(params, "GB2312"));
		HttpResponse httpResponse;
		httpResponse = client.execute(loginPost);
		
		int statecode = httpResponse.getStatusLine().getStatusCode();
		if (statecode == 302) {
			Header[] headers = httpResponse.getHeaders("Location");
			for (Header header : headers) {
				String redirectuUri = "http://jwgldx.gdut.edu.cn"
						+ header.getValue();
				this.panelUrl = redirectuUri;
				System.out.println(panelUrl);
			}
		}
		loginPost.abort();
		return "";
	}
/**
 * 
 * @param ref  **当前url
 * @return
 * @throws Exception
 */
	public String getGradeViewState(String ref) throws Exception {

		HttpGet get = new HttpGet(this.getGradeUrl);
		get.setHeader("Referer", ref);
		HttpResponse httpResponse;
		httpResponse = client.execute(get);
		String resulthtml = EduMangerUtil.convertStreamToString(httpResponse
				.getEntity().getContent());

		Document doc = Jsoup.parse(resulthtml);
		String vd = doc.select("input[name=__VIEWSTATE]").get(0).attr("value");
		this.viewstate = vd;

		get.abort();
		return vd;
	}
/**
 * 通过年份获取成绩
 * @param yearRange
 * @return
 * @throws Exception
 */
	public String getGradeByYears(String yearRange) throws Exception {
		String gradeUrl = this.getGradeUrl;
		List<NameValuePair> posGradeParams = new ArrayList<NameValuePair>();
		posGradeParams
				.add(new BasicNameValuePair("__VIEWSTATE", this.viewstate));
		posGradeParams.add(new BasicNameValuePair("ddlXQ", ""));
		posGradeParams.add(new BasicNameValuePair("Button5", "按学年查询"));
		posGradeParams.add(new BasicNameValuePair("ddlXN", yearRange));
		posGradeParams.add(new BasicNameValuePair("txtQSCJ", "0"));
		posGradeParams.add(new BasicNameValuePair("txtZZCJ", "100"));

		HttpPost postGrade = new HttpPost(gradeUrl);
		postGrade.setHeader("Referer", gradeUrl);
		HttpResponse httpResponse;
		postGrade.setEntity(new UrlEncodedFormEntity(posGradeParams, "GB2312"));
		httpResponse = client.execute(postGrade);
		String resulthtml = EduMangerUtil.convertStreamToString(httpResponse
				.getEntity().getContent());
		postGrade.abort();
		return resulthtml;

	}
/**
 * 计算绩点
 * @param yearsRange
 * @return
 * @throws Exception
 */
	public String getGradePoint(String yearsRange) throws Exception {

		this.getGradeViewState(this.loginUrl);
		String resul = this.getGradeByYears(yearsRange);
		String res = EduMangerUtil.parseGradeTable(resul);
		return res;
	}
/**
 * 获得课程表
 * @return
 * @throws ClientProtocolException
 * @throws IOException
 */
	public String getSelectCourceTable() throws ClientProtocolException,
			IOException {
		String scUrl = mainUrl + "xsxkqk.aspx?xh=" + username
				+ "&gnmkdm=N121615";
		HttpGet get = new HttpGet(scUrl);
		get.setHeader("Referer", panelUrl);
		HttpResponse httpResponse;
		httpResponse = client.execute(get);
		String resulthtml = EduMangerUtil.convertStreamToString(httpResponse
				.getEntity().getContent());
		Document doc = Jsoup.parse(resulthtml);
		String vd = doc.select(".datelist").get(0).html();
		vd="<table class='datelist'>"+vd+"</table>";
		return vd;
	}
/**
 * 测试
 * @param args
 * @throws Exception
 */
	public static void main(String[] args) throws Exception {
//		System.out.println(getGradePoint("3111004846", "asd5826190",
//				"2013-2014"));1
		
		
		BroswerX box = new BroswerX("3111004846", "asd5826190");

		System.out.println(box.getSelectCourceTable());
		
	}
}
