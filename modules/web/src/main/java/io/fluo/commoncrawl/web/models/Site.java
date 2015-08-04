package io.fluo.commoncrawl.web.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;

public class Site {

  @Length(max = 100)
  private String domain;

  @Length(max = 1000)
  private String lastPage;

  @Length(max = 1000)
  private String prevLastPage;

  boolean moreResults;

  private List<PageCount> pages = new ArrayList<>();

  public Site() {
    // Jackson deserialization
  }

  public Site(String domain, String prevLastPage) {
    this.domain = domain;
    this.prevLastPage = prevLastPage;
  }

  @JsonProperty
  public String getDomain() {
    return domain;
  }

  @JsonProperty
  public List<PageCount> getPages() {
    return pages;
  }

  @JsonProperty
  public String getLastPage() {
    return lastPage;
  }

  public void setLastPage(String lastPage) {
    this.lastPage = lastPage;
  }

  @JsonProperty
  public String getPrevLastPage() {
    return prevLastPage;
  }

  @JsonProperty
  public boolean getMoreResults() {
    return moreResults;
  }

  public void setMoreResults(boolean moreResults) {
    this.moreResults = moreResults;
  }

  public void addPage(PageCount pc) {
    pages.add(pc);
  }
}
