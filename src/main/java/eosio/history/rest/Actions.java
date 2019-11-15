package eosio.history.rest;

public class Actions {
      String account_name;
      int pos;
      int offset;
      String filter;
      String sort;
      String after;
      String before;
      String last;

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getAccount_name() {
        return account_name;
    }

    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    @Override
    public String toString() {
        return "Actions{" +
                "account_name='" + account_name + '\'' +
                ", pos=" + pos +
                ", offset=" + offset +
                ", filter='" + filter + '\'' +
                ", sort='" + sort + '\'' +
                ", after='" + after + '\'' +
                ", before='" + before + '\'' +
                '}';
    }
}
