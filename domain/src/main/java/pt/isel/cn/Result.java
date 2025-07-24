package pt.isel.cn;

import java.util.List;

public class Result {

    public Result() {}

    public List<MonumentDomain> monuments;

    public Result(List<MonumentDomain> monumentList) {
        this.monuments = monumentList;
    }
}
