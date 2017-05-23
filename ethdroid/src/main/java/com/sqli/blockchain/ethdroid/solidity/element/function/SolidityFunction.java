package com.sqli.blockchain.ethdroid.solidity.element.function;


import com.sqli.blockchain.ethdroid.EthDroid;
import com.sqli.blockchain.ethdroid.Utils;
import com.sqli.blockchain.ethdroid.exception.SmartContractException;
import com.sqli.blockchain.ethdroid.model.Filter;
import com.sqli.blockchain.ethdroid.model.Transaction;
import com.sqli.blockchain.ethdroid.solidity.coder.SCoder;
import com.sqli.blockchain.ethdroid.solidity.element.SolidityElement;
import com.sqli.blockchain.ethdroid.solidity.element.returns.SingleReturn;
import com.sqli.blockchain.ethdroid.solidity.types.SArray;
import com.sqli.blockchain.ethdroid.solidity.types.SType;

import org.ethereum.geth.Block;
import org.ethereum.geth.Hash;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Single;

/**
 * Created by gunicolas on 4/08/16.
 */

public class SolidityFunction<T extends SType> extends SolidityElement {

    protected Object[] args;

    public SolidityFunction(String address, Method method, EthDroid eth, Object[] args) {
        super(address, method, eth);
        this.args = args;
    }

    @Override
    protected List<AbstractMap.SimpleEntry<Type,SArray.Size>> getParametersType() {
        Type[] parametersType = method.getGenericParameterTypes();
        Annotation[][] parametersAnnotations = method.getParameterAnnotations();

        List<AbstractMap.SimpleEntry<Type,SArray.Size>> ret = new ArrayList<>();
        for(int i=0;i<parametersType.length;i++){
            SArray.Size arraySize = Utils.arrayContainsAnnotation(parametersAnnotations[i], SArray.Size.class);
            ret.add(new AbstractMap.SimpleEntry<>(parametersType[i], arraySize));
        }
        return ret;
    }

    @Override
    protected String signature() {
        return super.signature().substring(0, 8);
    }

    private String encode() {
        String encodedParameters = "";
        if (args != null) {
            encodedParameters = SCoder.encodeParams(args);
        }
        return "0x" + this.signature() + encodedParameters;
    }

    private Transaction buildTransaction()throws Exception {
        return eth.newTransaction()
            .to(address)
            .data(encode());
    }

    public Hash send() throws Exception {
        return buildTransaction().send();
    }
    public Observable<Block> sendWithNotification() throws Exception {
        return buildTransaction().sendWithNotification();
    }

    SType[] makeCallAndDecode() throws Exception{
        String hexadecimalResult = buildTransaction().call();

        if( returns.size() == 0 ) return null;
        if( hexadecimalResult.length() == 0 ) throw new SmartContractException();
        return SCoder.decodeParams(hexadecimalResult,returns);
    }

    public SingleReturn<T> call() throws Exception{
        SType[] results = makeCallAndDecode();
        return results != null ? new SingleReturn<>((T) results[0]) : null;
    }
}