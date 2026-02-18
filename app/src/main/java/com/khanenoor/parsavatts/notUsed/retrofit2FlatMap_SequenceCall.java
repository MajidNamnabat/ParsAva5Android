                    /*
                     * Suggest Please See this code:
                     * https://github.com/ReactiveX/RxJava/issues/4196
                     * https://stackoverflow.com/questions/38358767/iterating-over-network-api-call-using-retrofit2
                     *
                     */
                /*
                Disposable disposable = mParsWebService.GetProductKey(encryptHardwareAppId).flatMap(
                        productKeyResult -> mParsWebService.GetLicenseKey(productKeyResult,encryptHardwareAppId))
                        .to()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pair -> {
                            @SuppressLint("CheckResult") int returnCode = Lock.WriteInterpretedWebServiceResponse(packageName,pair);
                            if(returnCode!=0){
                                String stringId = "WebService_Error_" + String.valueOf(returnCode);
                                int resId = getResources().getIdentifier(stringId,"string", packageName);
                               //mLicenseErrors.setText(getString(resId));
                            } else {
                                //errTextDisplay = getResources().getString(R.string.WebService_Register_Succeed);
                                //mLicenseErrors.setText(errTextDisplay);
                            }
                            // this is reached only when fetchSales and fetchAds are done
                        }, throwable -> {
                            // this is reached when fetchSales or fetchAds throws an error
                        });
              CompositeDisposable disposeBag = new CompositeDisposable();
              disposeBag.add(disposable);

                 */
                /*
                class GetProductKey implements Runnable {
                    private volatile String value;
                    private volatile boolean isSucceed;
                    public String getValue() {
                        return value;
                    }
                    public boolean getIsSucceed(){return isSucceed;}
                    @Override
                    public void run() {
                        Call<String> FuncGetProductCall = mParsWebService.GetProductKey(encryptHardwareAppId);
                        try {
                            Response<String> funcResult = FuncGetProductCall.execute();
                            value = funcResult.body();
                            isSucceed=true;
                        } catch (IOException e) {
                            //throw new RuntimeException(e);
                            isSucceed=false;
                            LogUtils.w(TAG, "Retrofit call GetProductKey failed exception occurred : " + e.getMessage());
                        }
                    }
                }
                GetProductKey getProductKey = new GetProductKey();
                Thread threadSpeak = new Thread(getProductKey, "Product Key Receive Thread");
                threadSpeak.start();
                try {
                    threadSpeak.join();
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                    LogUtils.w(TAG,"Thread.join failed exception occurred : " + e.getMessage() );
                }

                if(!getProductKey.getIsSucceed()){
                    mLicenseErrors.setText(R.string.WebService_Register_CallFailed);
                    findViewById(R.id.license_register).setEnabled(true);
                    findViewById(R.id.license_receivelicensekey).setEnabled(true);
                    return;
                }
                productKey = getProductKey.value;
                mEdtSoftwareKey.setText(productKey);
                */
                /*
                mPrefs.set(APP_PRODUCT_KEY,productKey);
                new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        String encryptResponse = response.body();
                        int returnCode = Lock.WriteInterpretedWebServiceResponse(packageName,encryptResponse);
                        if(returnCode!=0){
                            String stringId = "WebService_Error_" + String.valueOf(returnCode);
                            int resId = getResources().getIdentifier(stringId,"string", packageName);
                            mLicenseErrors.setText(getString(resId));
                        } else {
                            String errTextDisplay = getResources().getString(R.string.WebService_ReceiverLicense_Succeed);
                            mLicenseErrors.setText(errTextDisplay);
                        }
                        findViewById(R.id.license_register).setEnabled(true);
                        findViewById(R.id.license_receivelicensekey).setEnabled(true);

                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        //EventBus.getDefault().post(new Error("Error: " + t.getMessage()));
                        LogUtils.w(TAG,"Call WebService Receive License failed");
                        String errTextDisplay = getResources().getString(R.string.WebService_Register_CallFailed);

                        mLicenseErrors.setText(errTextDisplay);
                        findViewById(R.id.license_register).setEnabled(true);
                        findViewById(R.id.license_receivelicensekey).setEnabled(true);

                    }
                });
            */
